import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import java.math.BigInteger;

import java.text.SimpleDateFormat;

import java.sql.Timestamp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.lang.reflect.InvocationTargetException;

import common.Constants;
import messages.MessageTypes;
import rsa.KeySet;
import rsa.RSA;

public class MultiThreadedClient {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// GUI Components
	//
	static JTextPane displayJTextPane = null;			// The upper text area where received messages are displayed
	static JTextPane inputJTextPane = null;				// The lower text area where messages are input
	//		
	// JScrollPanes for the JTextPanes
	static JScrollPane displayJTextScrollPane = null;	// Scroll pane for the upper text area
	static JScrollPane inputJTextScrollPane = null;		// Scroll pane for the lower text area
	//
	// Contacts list: data model and GUI component
	static DefaultListModel listModel = null;			// The data model, which holds ListItem instances  (ListItem is defined at the bottom of this file.)
	static JList list = null;							// The GUI component which displays ListItem.alias values
	//
	static JFrame jf = null;							// The main application window
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Server connection and key set
	//
	static Socket s = null;								// Socket for server i/o :
	static DataInputStream dis = null;					//    Input from server
	static DataOutputStream dos = null;					//    Output to server
	//
	static String serverIPAddress = null;				// IPv4 address of the chat server (This is hard coded in main, but later may be configurable.)
	static int serverPort = -1;							// Port of the chat server (This is hard coded in main, but later may be configurable.)
	//
	static BigInteger serverPublicKeyPartI = null;		// Server's public modulus n, received from server at connection
	static BigInteger serverPublicKeyPartII = null;		// Server's public exponent e, received from server at connection
	//
	static volatile boolean disconnected = true;		// The client will attempt to (re)connect if disconnected == true .
	//
	static final long MAX_IO_LAPSE_MILLIS = 60000;		// If System.currentTimeMillis() - timeOfMostRecentServerIOoperationEpochMillis > MAX_IO_LAPSE_MILLIS is observed by the pulse monitor, disconnected will get set to true.
	//	
	static volatile long timeOfMostRecentServerIOoperationEpochMillis = Long.MAX_VALUE;	// If System.currentTimeMillis() - timeOfMostRecentServerIOoperationEpochMillis > MAX_IO_LAPSE_MILLIS is observed by the pulse monitor, disconnected will get set to true.
	//
	static final long RECONNECTION_DELAY = 5000;
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Client key set, ID, and alias
	//
	static KeySet clientKeySet = null;		// Read from private.txt and public.txt, or generated if public.txt or private.txt does not exist
	static String publicKeyHashID = null;	// Our ID, generated from a hash of our public key
	static String alias = null;				// Our alias (for local display only), equal to the first field in public.txt	
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Contacts keys, and aliases
	//
	static HashMap<String, String> pkDir = new HashMap<String, String>();					// Holds publicKeyHashID:publicKey pairs for all connected clients received from the server during the session
	static HashMap<String, String> contactsPkDir = new HashMap<String, String>();			// Holds publicKeyHashID:publicKey pairs retrieved from files stored in contacts/ , and our publicKeyHashID:publicKey so we can chat with ourself (e.g. to test connectivity with the server)
	static volatile String currentContactPKH = null;										// The public key hash ID of the current contact
	static HashMap<String, String> publicKeyHashIDAliasMap = new HashMap<String, String>();	// Mapping of public key hashes to aliases, for contacts and ourself
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Messaging and display
	//
	static SimpleDateFormat sdf = null;				// A UTC date time stamp with millisecond precision is included in each message, to prevent replay attacks, and uniquely identify messages.
	static String SimpleDateFormatString = null;	// This is equal to the first line of settings/datetime.txt , which is generated with a default string if it doesn't exist.
	//
	static long userBeganTyping = 0;				// This holds a millisecond count used to send a typing indicator messages every 5 seconds while the user is typing.
	//
	static int displayTextFontSize = 14;			// TODO: Read from a config file (Currently, the user can use ctrl+mouse wheel to change the font size during the session.)
	static int inputTextFontSize = 14;				// TODO: Read from a config file (Currently, the user can use ctrl+mouse wheel to change the font size during the session.)
	//
	static Object renderLock = new Object();		// For synchronizing chat logging to file and to the display area
	//
	static final long PULSE_DELAY = 1000;			// Milliseconds between pulses sent to the server
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// MAGIC CODES: The security of the system does not depend on these codes.  These are only used locally for rendering purposes.  
	// Messages containing any of these codes are just ignored.  TODO: They should raise a warning of some sort.
	//
	public static final String ACK =    "aj93j8nfnvj902lajnvng74aldkf93hfAhfA33HaSJVJ92haDSjhaAdhckaweriaoiasdh3904732rwiohlskfdmvxbvmSKLFHKLHSDFSLKDVN329073HLKF";
	public static final String SIG =    "ksfd83khlsg897ow3hkjsghkjsdfmxmalkaaoif8923iyesjgvmKJSF32897KJDSGKJVDSKHDDDSI78324ggsgbvdiw3iokU32968SFKJVBKJKHkjsj32w01";
	public static final String INVSIG = "KJSGDKJDSGSKJHo23i4ojkgsvbkkbSFOIH854UKLHTGFJKVSMBXMBSDUHuhsfdf98345kgsdhkvxhkdsfhwet986yfdshkjIESFOIOIW723OISDHohds857d";
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// File I/O
	//
	public static final String startupDirectoryNameString = new File("").getAbsolutePath();	// The absolute path of the application's startup directory
	//
	public static File contactsFolder;		// Contains a public key file for each contact
	public static File chatFolder;			// Contains a chat history for each contact
	public static File keysetFolder;		// Contains this client's public and private key files
	public static File settingsFolder;		// Contains a datetime.txt file to set the simple date format string
	public static File soundFolder;			// Contains the "message received" sound file
	public static File imagesFolder;		// Contains image icons for ack, valid signature, and invalid signature, as well as the the task bar icons and a shortcut .ico
	public static File rcvFolder;			// Contains received files
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Object consoleLock = new Object();	// For synchronizing debugging and logging to System.out
	public static final String VERSION = "v.0.17.1";

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
		serverIPAddress = "216.231.128.40";
		//		serverIPAddress = "127.0.0.1";
		serverPort = 11177;

		// Read in the client's public and private keys, or generate them.  Read in any contacts' public keys, the date time format, etc.
		configure();

		// Schedule a job- vis. creating and showing the GUI- for the AWT event dispatching thread.
		javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Connect to the server.  In case the network connection fails or the server is restarted, 
		// attempt to reconnect to the server once every RECONNECTION_DELAY seconds for as long as the application is running.
		//
		Thread t = new Thread() {
			public void run() {
				connectionLoop();
			}
		};
		t.start();
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	}

	private static void connectionLoop() {
		int connectionAttempt = -1;
		String dots = null;

		while(true) {
			if (connectionAttempt != -1) try {Thread.sleep(RECONNECTION_DELAY);} catch (InterruptedException e1) {}

			if (!disconnected) continue;

			timeOfMostRecentServerIOoperationEpochMillis = Long.MAX_VALUE;

			for (int i = 0; i < listModel.size(); i++) {
				((ListItem)listModel.elementAt(i)).setLoggedIn(0);
			}
			list.repaint(10);

			if (connectionAttempt == Integer.MAX_VALUE) connectionAttempt = -1;
			connectionAttempt++;
			dots = "";
			for (int i = 0; i < connectionAttempt % 4; i++) {
				dots = dots + ".";
			}
			jf.setTitle("Radchat" + " " + VERSION  + " " + "(" + alias + ")" + "   " + "[Attempting to connect to server" + dots + "]");

			//////////////////////////////////////
			//									//
			// 		  Message connection		//
			//									//
			//////////////////////////////////////

			s = null;
			try {
				s = new Socket(serverIPAddress, serverPort);
				s.setSoTimeout(0);
				dos = new DataOutputStream(s.getOutputStream());
			} 
			catch (Exception e) {
				e.printStackTrace();
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				disconnected = true;
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;
			}
			disconnected = false;

			jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[Connecting]");

			Thread serverPulseMonitor = new Thread() {
				public void run() {
					while (!disconnected) {
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (System.currentTimeMillis() - timeOfMostRecentServerIOoperationEpochMillis > MAX_IO_LAPSE_MILLIS) {
							disconnected = true;
							if (s != null) {
								try {
									s.close();
								} 
								catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							if (dis != null) {
								try {
									dis.close();
								} 
								catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							if (dos != null) {
								try {
									dos.close();
								} 
								catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							list.repaint(10);
							inputJTextPane.setEditable(false);
							return;
						}
					}
				}
			};
			serverPulseMonitor.start();
			//

			//////////////////////////////////////
			//
			// Send the connection type to the server.
			//
			try {
				if (disconnected) throw new Exception();
				dos.writeUTF(MessageTypes.USER);
				dos.flush();
				timeOfMostRecentServerIOoperationEpochMillis = System.currentTimeMillis();
			} 
			catch (Exception e) {
				e.printStackTrace();
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				disconnected = true;
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;
			}		
			//
			//////////////////////////////////////

			//////////////////////////////////////
			//
			// Send our public key to the server.
			//
			try {
				if (disconnected) throw new Exception();
				dos.writeUTF(clientKeySet.n.toString() + ":" + clientKeySet.e.toString());
				dos.flush();
				timeOfMostRecentServerIOoperationEpochMillis = System.currentTimeMillis();
			} 
			catch (Exception e) {
				e.printStackTrace();
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				disconnected = true;
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;
			}		
			//
			//////////////////////////////////////

			jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[Connecting.]");

			//////////////////////////////////////
			//
			// Receive the server's public key.
			//
			try {
				if (disconnected) throw new Exception();
				dis = new DataInputStream(s.getInputStream());
				String messageFromServer = dis.readUTF();
				timeOfMostRecentServerIOoperationEpochMillis = System.currentTimeMillis();
				serverPublicKeyPartI = new BigInteger(messageFromServer.split(":")[0]);
				serverPublicKeyPartII = new BigInteger(messageFromServer.split(":")[1]);
			} 
			catch (Exception e) {
				e.printStackTrace();
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				disconnected = true;
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;		
			}

			jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[Connecting..]");

			//////////////////////////////////////
			//
			// Now that we have exchanged keys with the server, send our name encrypted to the server.
			//
			try {
				if (disconnected) throw new Exception();
				dos.writeUTF(RSA.encryptMessage(publicKeyHashID, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits));
				dos.flush();
				timeOfMostRecentServerIOoperationEpochMillis = System.currentTimeMillis();
			} 
			catch (Exception e) {
				e.printStackTrace();
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					}
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				disconnected = true;
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;		
			}		
			//
			//////////////////////////////////////

			jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[Connecting...] (Awaiting initial pulse from server)");

			disconnected = false;

			MessageListener messageListener = new MessageListener();
			messageListener.start();
			//
			// Await the first server pulse
			//
			int seconds = 0;
			timeOfMostRecentServerIOoperationEpochMillis = Long.MAX_VALUE;
			while (timeOfMostRecentServerIOoperationEpochMillis == Long.MAX_VALUE && seconds < 10) {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				seconds++;
			}
			if (timeOfMostRecentServerIOoperationEpochMillis == Long.MAX_VALUE) {
				disconnected = true;
				if (s != null) {
					try {
						s.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (dos != null) {
					try {
						dos.close();
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				list.repaint(10);
				inputJTextPane.setEditable(false);
				continue;
			}

			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//
			// At this point we have established a TCP/IP connection and completed an application layer handshake with the server, 
			// so update the UI and start the pulse generator.
			//
			inputJTextPane.setEditable(true);
			jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[" + "Chatting with " + publicKeyHashIDAliasMap.get(currentContactPKH) + "]");
			list.repaint(10);
			//
			Thread pulseGenerator = new Thread("RADCHAT_Pulse_Generator") {
				public void run() {
					while (!disconnected) {
						try {			
							String pulseString = MessageTypes.PULSE;
							synchronized(dos) {
								dos.writeUTF(pulseString);	
								dos.flush();
							}
						} 
						catch (Exception e) {
							disconnected = true;
							return;
						}
						try {Thread.sleep(PULSE_DELAY);} catch (InterruptedException e) {}
					}
				}
			};
			pulseGenerator.start();
			//
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		}
	}

	public static void displayAllThreads() {
		synchronized(consoleLock) {
			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
			System.out.println("\n-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
			System.out.println("All Threads: \n");
			System.out.println("id" + " | " + "name" + " | " + "state" + " | " + "alive");
			for (Thread t : threadSet) {
				System.out.println(t.getId() + " | " + t.getName() + " | " + t.getState() + " | " + t.isAlive());
			}
			System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");
			System.out.flush();
		}
	}

	private static void configure() {
		contactsFolder = new File(startupDirectoryNameString + File.separator + "contacts");
		if (!contactsFolder.exists()) {
			contactsFolder.mkdir();
		}

		chatFolder = new File(startupDirectoryNameString + File.separator + "chat");
		if (!chatFolder.exists()) {
			chatFolder.mkdir();
		}

		keysetFolder = new File(startupDirectoryNameString + File.separator + "keyset");
		if (!keysetFolder.exists()) {
			keysetFolder.mkdir();
		}

		settingsFolder = new File(startupDirectoryNameString + File.separator + "settings");
		if (!settingsFolder.exists()) {
			settingsFolder.mkdir();
		}

		soundFolder = new File(startupDirectoryNameString + File.separator + "sound");
		if (!soundFolder.exists()) {
			soundFolder.mkdir();
		}

		imagesFolder = new File(startupDirectoryNameString + File.separator + "images");
		if (!imagesFolder.exists()) {
			imagesFolder.mkdir();
		}

		rcvFolder = new File(startupDirectoryNameString + File.separator + "rcv");
		if (!rcvFolder.exists()) {
			rcvFolder.mkdir();
		}

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Read in date time format or generate a default datetime.txt if a valid one does not exist.
		//
		File datetimeFile = new File(settingsFolder.getAbsolutePath() + File.separator + "datetime.txt");
		//
		if (datetimeFile.exists()) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(datetimeFile), "UTF-8"));
				SimpleDateFormatString = in.readLine();
			} 
			catch (UnsupportedEncodingException e) 
			{
				System.out.println(e.getMessage());
			} 
			catch (IOException e) 
			{
				System.out.println(e.getMessage());
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
			finally {
				if (in != null) {
					try {
						in.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				sdf = new SimpleDateFormat(SimpleDateFormatString);
			}
			catch (Exception e) {
				e.printStackTrace();
				sdf = null;
			}
		}
		if (sdf == null) {
			SimpleDateFormatString = "EEE, dd MMM yyyy HH:mm:ss.SSS zzz";

			File newDatetimeFile;
			FileOutputStream newDatetimeFOS = null;

			try {
				newDatetimeFile = new File(settingsFolder.getAbsolutePath() +  File.separator + "datetime.txt");
				newDatetimeFOS = new FileOutputStream(newDatetimeFile);

				if (newDatetimeFile.exists()) {
					// If there is already a datetime file, the format string is invalid so delete (TODO: rename) it and create a new empty file in which to store the default format string.
					newDatetimeFile.delete();
					newDatetimeFile.createNewFile();
				}
				else {
					// Otherwise just create a new empty file
					newDatetimeFile.createNewFile();
				}

				// get the content in bytes
				byte[] content5InBytes = SimpleDateFormatString.getBytes();

				newDatetimeFOS.write(content5InBytes);
				newDatetimeFOS.flush();
				newDatetimeFOS.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			} 
			finally {
				try {
					if (newDatetimeFOS != null) {
						newDatetimeFOS.close();
					}
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		sdf = new SimpleDateFormat(SimpleDateFormatString);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));  // The UTC time is sent with each message rather than the user's local time to not give a clue about their location.

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Read in our key set or generate one if there are not valid public.txt and private.txt files.
		//
		File publicKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "public.txt");
		File privateKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "private.txt");

		if (publicKeyFile.exists() && privateKeyFile.exists()) {
			BufferedReader in1 = null;
			BufferedReader in2 = null;
			try {
				in1 = new BufferedReader(new InputStreamReader(new FileInputStream(publicKeyFile), "UTF-8"));
				in2 = new BufferedReader(new InputStreamReader(new FileInputStream(privateKeyFile), "UTF-8"));
				String publicKey[] = in1.readLine().split(":");  	
				clientKeySet = new KeySet(new BigInteger(publicKey[1]), new BigInteger(publicKey[2]), new BigInteger(in2.readLine()));
				alias = publicKey[0];
				/////
				//
				// Set our publicKeyHashID
				//
				try {
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte[] hashInBytes = md.digest(((clientKeySet.n).toString()).getBytes("UTF-8"));
					//
					// bytes to hex (TODO: Maybe this can be optimized)
					//
					StringBuilder sb = new StringBuilder();
					for (byte b : hashInBytes) {
						sb.append(String.format("%02x", b));
					}
					publicKeyHashID = sb.toString();
				} 
				catch (NoSuchAlgorithmException e1) {
					e1.printStackTrace();
				}
				catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				//
				/////
			} 
			catch (UnsupportedEncodingException e) 
			{
				System.out.println(e.getMessage());
			} 
			catch (IOException e) 
			{
				System.out.println(e.getMessage());
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
			finally {
				if (in1 != null) {
					try {
						in1.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (in2 != null) {
					try {
						in2.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO: Validate the key set.  If it's invalid then set publicKeyHashID = null , so that a publicKeyHashID and key set will be generated.
		// TODO: Validate the generated set also, in a loop, to ensure a functioning key set is generated.
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if (publicKeyHashID == null) {
			clientKeySet = RSA.generate(Constants.keySizeBits);

			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//
			// Set our name to a hash of our public key.
			//
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				byte[] hashInBytes = md.digest(((clientKeySet.n).toString()).getBytes("UTF-8"));
				//
				// bytes to hex (TODO: Maybe this can be optimized.)
				//
				StringBuilder sb = new StringBuilder();
				for (byte b : hashInBytes) {
					sb.append(String.format("%02x", b));
				}
				publicKeyHashID = sb.toString();
				alias = publicKeyHashID;
			} 
			catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			}
			catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			//
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			File newPublicKeyFile = null;
			FileOutputStream newPublicKeyFOS = null;
			String newPublicKeyFileContent = alias + ":" + clientKeySet.n + ":" + clientKeySet.e + "\n";

			File newPrivateKeyFile;
			FileOutputStream newPrivateKeyFOS = null;
			String newPrivateKeyFileContent = clientKeySet.d.toString() + "\n";

			try {
				newPublicKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "public.txt");
				newPrivateKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "private.txt");

				if (newPublicKeyFile.exists() || newPrivateKeyFile.exists()) {					
					// If there are already key files, the key set is invalid so delete (TODO: rename) it and create new empty files in which to store the generated key set.
					newPublicKeyFile.delete();
					newPublicKeyFile.createNewFile();

					newPrivateKeyFile.delete();
					newPrivateKeyFile.createNewFile();
				}
				else {
					// Otherwise just create new empty files
					newPublicKeyFile.createNewFile();
					newPrivateKeyFile.createNewFile();
				}

				// get the content in bytes
				byte[] newPublicKeyFileContentInBytes = newPublicKeyFileContent.getBytes();
				byte[] newPrivateKeyFileContentInBytes = newPrivateKeyFileContent.getBytes();
				
				newPublicKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "public.txt");
				newPrivateKeyFile = new File(keysetFolder.getAbsolutePath() + File.separator + "private.txt");
				
				newPublicKeyFOS = new FileOutputStream(newPublicKeyFile);
				newPublicKeyFOS.write(newPublicKeyFileContentInBytes);
				newPublicKeyFOS.flush();
				newPublicKeyFOS.close();

				newPrivateKeyFOS = new FileOutputStream(newPrivateKeyFile);
				newPrivateKeyFOS.write(newPrivateKeyFileContentInBytes);
				newPrivateKeyFOS.flush();
				newPrivateKeyFOS.close();

				System.out.println("Done");
			} 
			catch (IOException e) {
				e.printStackTrace();
			} 
			finally {
				try {
					if (newPublicKeyFOS != null) {
						newPublicKeyFOS.close();
					}
					if (newPrivateKeyFOS != null) {
						newPrivateKeyFOS.close();
					}
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Read in the contacts keys
		//
		File[] contactFiles = contactsFolder.listFiles(); 
		//
		contactsPkDir.put(publicKeyHashID, clientKeySet.n + ":" + clientKeySet.e);  // Insert our own ID and key into the map so we can chat with ourself.
		publicKeyHashIDAliasMap.put(publicKeyHashID, alias);
		//
		for (File contactFile : contactFiles) 
		{
			if (contactFile.isFile()) 
			{
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(new FileInputStream(contactFile), "UTF-8"));
					String userKeyPair;
					while ((userKeyPair = in.readLine()) != null) {
						String publicKeyHash = "";
						/////
						//
						// Hash the public key to generate the name.
						//
						try {
							MessageDigest md = MessageDigest.getInstance("SHA-256");
							byte[] hashInBytes = md.digest(userKeyPair.split(":")[1].toString().getBytes("UTF-8"));
							//
							// bytes to hex (TODO: Maybe this can be optimized.)
							//
							StringBuilder sb = new StringBuilder();
							for (byte b : hashInBytes) {
								sb.append(String.format("%02x", b));
							}
							publicKeyHash = sb.toString();
						} 
						catch (NoSuchAlgorithmException e1) {
							e1.printStackTrace();
						}
						catch (UnsupportedEncodingException e1) {
							e1.printStackTrace();
						}
						contactsPkDir.put(publicKeyHash, userKeyPair.split(":")[1] + ":" + userKeyPair.split(":")[2]);
						publicKeyHashIDAliasMap.put(publicKeyHash, userKeyPair.split(":")[0]);
					}
				} 
				catch (UnsupportedEncodingException e) 
				{
					System.out.println(e.getMessage());
				} 
				catch (IOException e) 
				{
					System.out.println(e.getMessage());
				}
				catch (Exception e)
				{
					System.out.println(e.getMessage());
				}
				finally {
					if (in != null) {
						try {
							in.close();
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}		   
			}
		}
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// For each contact, create a chat file if one does not exist.
		//
		for (String clientName : contactsPkDir.keySet()) {
			File chatFile = new File(chatFolder.getAbsolutePath() + File.separator + clientName);
			if (!chatFile.exists()) {
				try {
					chatFile.createNewFile();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	}

	@SuppressWarnings("serial")
	private static void createAndShowGUI() {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
			try {
				dos.writeUTF(RSA.encryptMessage(MessageTypes.BYE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits) + ":0" + ":asdf" + ":asdf");
				dos.flush();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			}
			System.exit(1);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			try {
				dos.writeUTF(RSA.encryptMessage(MessageTypes.BYE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits) + ":0" + ":asdf" + ":asdf");
				dos.flush();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			}
			System.exit(1);	
		}
		catch (InstantiationException e) {
			e.printStackTrace();
			try {
				dos.writeUTF(RSA.encryptMessage(MessageTypes.BYE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits) + ":0" + ":asdf" + ":asdf");
				dos.flush();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			}
			System.exit(1);	
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
			try {
				dos.writeUTF(RSA.encryptMessage(MessageTypes.BYE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits) + ":0" + ":asdf" + ":asdf");
				dos.flush();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			}
			System.exit(1);
		}

		JFrame.setDefaultLookAndFeelDecorated(true);
		jf = new JFrame();
		jf.setLayout(new BorderLayout());

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Configure the display and input text panes.
		//
		final int displayTextAreaRows = 25;
		final int displayTextAreaCols = 60;
		//
		final int inputTextAreaRows = 5;
		final int inputTextAreaCols = displayTextAreaCols;
		//
		// Display pane
		//
		displayJTextPane = new JTextPane();
		displayJTextPane.setEditable(false);
		displayJTextPane.setContentType("text/plain");
		//
		displayJTextScrollPane = new JScrollPane(displayJTextPane);
		displayJTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		displayJTextScrollPane.setPreferredSize(new Dimension(1000, 500));
		displayJTextScrollPane.setMinimumSize(new Dimension(100, 50));
		//
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		//
		Style regular = displayJTextPane.addStyle("regular", def);
		StyleConstants.setFontFamily(def, Font.MONOSPACED);
		//
		Style contactStyle = displayJTextPane.addStyle("Contact", def);
		StyleConstants.setForeground(contactStyle, Color.blue);
		//
		Style userStyle = displayJTextPane.addStyle("User", def);
		StyleConstants.setForeground(userStyle, Color.gray);
		//
		Style ackStyle = displayJTextPane.addStyle("ack", regular);
		StyleConstants.setAlignment(ackStyle, StyleConstants.ALIGN_CENTER);
		ImageIcon icon = createImageIcon("ack.png", "ack");
		if (icon != null) {
			StyleConstants.setIcon(ackStyle, icon);
		}
		//
		Style validSigStyle = displayJTextPane.addStyle("validSig", regular);
		StyleConstants.setAlignment(validSigStyle, StyleConstants.ALIGN_CENTER);
		icon = createImageIcon("validsig.png", "validSig");
		if (icon != null) {
			StyleConstants.setIcon(validSigStyle, icon);
		}
		//
		Style invalidSigStyle = displayJTextPane.addStyle("invalidSig", regular);
		StyleConstants.setAlignment(invalidSigStyle, StyleConstants.ALIGN_CENTER);
		icon = createImageIcon("invalidsig.png", "invalidSig");
		if (icon != null) {
			StyleConstants.setIcon(invalidSigStyle, icon);
		}
		//
		Font displayTextAreaFont = new Font(Font.MONOSPACED, Font.PLAIN, displayTextFontSize);
		displayJTextPane.setFont(displayTextAreaFont);
		//
		FontMetrics displayTextAreaFontMetrics = displayJTextPane.getFontMetrics(displayTextAreaFont);
		int displayTextAreaFontHeight = displayTextAreaFontMetrics.getHeight();
		int displayTextAreaFontWidth = displayTextAreaFontMetrics.charWidth('a');  // Font.MONOSPACED width should be the same for all chars
		//
		// Input pane
		//
		inputJTextPane = new JTextPane();
		inputJTextPane.setEditable(true);
		inputJTextPane.setContentType("text/plain");
		//
		inputJTextScrollPane = new JScrollPane(inputJTextPane);
		inputJTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		inputJTextScrollPane.setPreferredSize(new Dimension(1000, 50));
		inputJTextScrollPane.setMinimumSize(new Dimension(100, 50));
		//
		Font inputTextAreaFont = new Font(Font.MONOSPACED, Font.PLAIN, inputTextFontSize);
		inputJTextPane.setFont(inputTextAreaFont);
		//
		FontMetrics inputTextAreaFontMetrics = displayJTextPane.getFontMetrics(inputTextAreaFont);
		int inputTextAreaFontHeight = inputTextAreaFontMetrics.getHeight();
		int inputTextAreaFontWidth = inputTextAreaFontMetrics.charWidth('a');	   // Font.MONOSPACED width should be the same for all chars
		//
		jf.setSize(displayTextAreaCols * displayTextAreaFontWidth + inputTextAreaCols * inputTextAreaFontWidth, displayTextAreaRows * displayTextAreaFontHeight + inputTextAreaRows * inputTextAreaFontHeight);
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// Put the display scroll pane and the input scroll pane in a split pane
		JSplitPane editorsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,	displayJTextScrollPane,	inputJTextScrollPane);
		editorsSplitPane.setOneTouchExpandable(true);
		editorsSplitPane.setResizeWeight(1);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Create contacts list data model and GUI component.  Attach a listener to the list.
		//
		listModel = new DefaultListModel();
		for (String clientName : contactsPkDir.keySet()) {
			listModel.addElement(new ListItem(publicKeyHashIDAliasMap.get(clientName), clientName));
		}
		//
		//Create the list and put it in a scroll pane.
		//
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		//
		currentContactPKH = ((ListItem)(listModel.get(list.getSelectedIndex()))).getPublicKeyHashID();
		//
		jf.setTitle("Radchat" + " " + VERSION + " " + "(" + alias + ")" + "   " + "[Chatting with " + publicKeyHashIDAliasMap.get(currentContactPKH) + "]");
		//
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				synchronized (renderLock) {
					currentContactPKH = ((ListItem)(listModel.get(list.getSelectedIndex()))).getPublicKeyHashID();
					if (!disconnected) jf.setTitle("Radchat" + " " + VERSION + " " + "(" + publicKeyHashIDAliasMap.get(publicKeyHashID) + ")" + "   " + "[Chatting with " + publicKeyHashIDAliasMap.get(currentContactPKH) + "]");
					File chatFile = new File(chatFolder.getAbsolutePath() + File.separator + currentContactPKH);

					displayJTextScrollPane.setVisible(false);
					displayJTextPane.setText("");

					StyledDocument docc =  displayJTextPane.getStyledDocument();
					displayJTextPane.setStyledDocument(new DefaultStyledDocument());

					FileInputStream fis = null;
					InputStreamReader isr = null;
					BufferedReader in = null;
					try {
						fis = new FileInputStream(chatFile);
						isr = new InputStreamReader(fis, "UTF-8");
						in = new BufferedReader(isr);

						String currentStyleString = "regular";

						String displayChatText = "";
						String line = "";
						while ((line = in.readLine()) != null) {
							displayChatText = line;
							try {
								if (line.startsWith(publicKeyHashID)) {
									currentStyleString = "User";
								}
								else if (line.startsWith(currentContactPKH)) {
									currentStyleString = "Contact";
								}
								else {
									// currentStyleString = currentStyleString;
								}

								for (String publicKeyHashID : publicKeyHashIDAliasMap.keySet()) {
									displayChatText = displayChatText.replace("(" + SIG + " " + publicKeyHashID + ")", "");	
								}
								displayChatText = displayChatText.replace("(" + INVSIG + ")", "");	

								for (String publicKeyHashID : publicKeyHashIDAliasMap.keySet()) {
									displayChatText = displayChatText.replace("[" + publicKeyHashID + " " + ACK + "]", "");
								}
								for (String publicKeyHashID : publicKeyHashIDAliasMap.keySet()) {
									displayChatText = displayChatText.replace(publicKeyHashID, publicKeyHashIDAliasMap.get(publicKeyHashID));				
								}
								docc.insertString(docc.getLength(), displayChatText, docc.getStyle(currentStyleString));

								if (line.contains(ACK)) {
									docc.insertString(docc.getLength(), " ", docc.getStyle("ack"));
								}
								if (line.contains(SIG)) {
									docc.insertString(docc.getLength(), " ", docc.getStyle("regular"));
									docc.insertString(docc.getLength(), " ", docc.getStyle("validSig"));
								}
								if (line.contains(INVSIG)) {
									docc.insertString(docc.getLength(), " ", docc.getStyle("regular"));
									docc.insertString(docc.getLength(), " ", docc.getStyle("invalidSig"));
								}
								docc.insertString(docc.getLength(), "\n", docc.getStyle("regular"));
							} 
							catch (BadLocationException e2) {
								e2.printStackTrace();
							}
						}
						displayJTextPane.setStyledDocument(docc);
						displayJTextPane.setCaretPosition(docc.getLength());  // Scroll to bottom
						displayJTextScrollPane.setVisible(true);
						((ListItem)(listModel.get(list.getSelectedIndex()))).setNewMessage(false);
					} 
					catch (IOException ex) {
						ex.printStackTrace();
					} 
					finally {
						if (in != null) {
							try {
								in.close();
								in = null;
							} 
							catch (IOException ex) {
								ex.printStackTrace();
							}
						}
						if (isr != null) {
							try {
								isr.close();
								isr = null;
							} 
							catch (IOException ex) {
								ex.printStackTrace();
							}
						}
						if (fis != null) {
							try {
								fis.close();
								fis = null;
							} 
							catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
		});
		list.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof ListItem) {
					ListItem nextUser = (ListItem) value;
					setText(nextUser.getDisplayText());
					if (nextUser.getLoggedIn() > 0 || (nextUser.getPublicKeyHashID().equals(publicKeyHashID) && !disconnected)) {
						setBackground(Color.GREEN);
					} 
					else {
						setBackground(Color.WHITE);
					}
					if (isSelected) {
						setBackground(getBackground().darker());
					}
				} 
				else {
					setText("whodat?");
				}
				return c;
			}
		});
		list.setVisibleRowCount(displayTextAreaRows + inputTextAreaRows);
		JScrollPane listScrollPane = new JScrollPane(list);
		//
		Container cp = jf.getContentPane();
		//
		cp.add(listScrollPane, BorderLayout.WEST);
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// Put the editors split pane and the contacts list scroll pane in a split pane.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, editorsSplitPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.2);

		// Add the final split pane to the main JFrame's content panel
		cp.add(splitPane, BorderLayout.CENTER);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Attach mouse wheel listeners to the input and display text panes so the fonts can be resized.
		//
		displayJTextPane.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					if (e.getWheelRotation() < 0) {
						displayTextFontSize++;
						Font f = new Font(Font.MONOSPACED, Font.PLAIN, displayTextFontSize);
						displayJTextPane.setFont(f);
						displayJTextPane.setCaretPosition(displayJTextPane.getDocument().getLength());  // Scroll to bottom
					}
					else if (e.getWheelRotation() > 0) {
						displayTextFontSize--;
						Font f = new Font(Font.MONOSPACED, Font.PLAIN, displayTextFontSize);
						displayJTextPane.setFont(f);
						displayJTextPane.setCaretPosition(displayJTextPane.getDocument().getLength());  // Scroll to bottom
					}
					else {
						inputJTextPane.getParent().dispatchEvent(e);
					}
				}
				else {
					displayJTextPane.getParent().dispatchEvent(e);
				}
			}
		});
		//
		inputJTextPane.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					if (e.getWheelRotation() < 0) {
						inputTextFontSize++;
						Font f = new Font(Font.MONOSPACED, Font.PLAIN, inputTextFontSize);
						inputJTextPane.setFont(f);
					}
					else if (e.getWheelRotation() > 0) {
						inputTextFontSize--;
						Font f = new Font(Font.MONOSPACED, Font.PLAIN, inputTextFontSize);
						inputJTextPane.setFont(f);
					}
					else {
						inputJTextPane.getParent().dispatchEvent(e);
					}
				}
				else {
					inputJTextPane.getParent().dispatchEvent(e);
				}
			}
		});
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Handle keyboard input in the input text field.
		//
		inputJTextPane.addKeyListener(new KeyListener () {
			public void keyTyped(KeyEvent e) {}

			public void keyPressed(KeyEvent e) {
				if (disconnected) {
					list.repaint(10);
					return;
				}
				try {
					//////////////////////////////////////
					//
					// Typing Indicator message
					//
					if (System.currentTimeMillis() - userBeganTyping > 5000) {
						userBeganTyping = System.currentTimeMillis();
						try {
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());
							String typingMessage = publicKeyHashID + ": " + sdf.format(timestamp) + ": " + "asd";
							String clientName = null;
							if (currentContactPKH != null) {
								clientName = currentContactPKH;
								String cMessage = RSA.encryptMessage(typingMessage, new BigInteger(contactsPkDir.get(clientName).split(":")[0]), new BigInteger(contactsPkDir.get(clientName).split(":")[1]), Constants.keySizeBits);
								String cClientName = RSA.encryptMessage(clientName, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);
								String cTYPING = RSA.encryptMessage(MessageTypes.TYPING, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);
								/////
								//
								// sign the message hash
								//
								MessageDigest md = MessageDigest.getInstance("SHA-256");
								byte[] hashInBytes = md.digest(typingMessage.getBytes("UTF-8"));
								//
								// bytes to hex
								//
								StringBuilder sb = new StringBuilder();
								for (byte b : hashInBytes) {
									sb.append(String.format("%02x", b));
								}
								String hash = sb.toString();
								String signature = RSA.encryptMessage(hash, clientKeySet.n, clientKeySet.d, Constants.keySizeBits);
								//
								/////
								dos.writeUTF(cTYPING + ":" + cClientName + ":" + cMessage + ":" + signature);	
								dos.flush();
							}
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						} 
						catch (NoSuchAlgorithmException e1) {
							e1.printStackTrace();
						}
					}
					//
					//////////////////////////////////////

					//////////////////////////////////////
					//
					// Allow the user to input newlines in the input text area, and transmit messages that do not trim() to "" to the server.
					//
					synchronized (renderLock) {
						String message = "";
						if (e.getKeyChar() == '\n') {
							e.consume();
							if (e.isShiftDown()) {
								inputJTextPane.getDocument().insertString(inputJTextPane.getCaretPosition(), "\n", StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE));
							}
							else {
								if (inputJTextPane.getText().trim().equals("")) {
									return;
								}
								if (inputJTextPane.getText().contains(ACK) || inputJTextPane.getText().contains(SIG) ||inputJTextPane.getText().contains(INVSIG)) {
									return;
								}
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								message = publicKeyHashID + ": " + sdf.format(timestamp) + ": " + inputJTextPane.getText();
								String displayMessage = publicKeyHashIDAliasMap.get(publicKeyHashID)  + ": " + sdf.format(timestamp) + ": " + inputJTextPane.getText() + "\n";

								File chatFile = new File(chatFolder.getAbsolutePath() + File.separator + currentContactPKH);
								appendToFile(chatFile, message + "\n");
								inputJTextPane.setText("");
								StyledDocument doc = displayJTextPane.getStyledDocument();
								try {
									doc.insertString(doc.getLength(), displayMessage, doc.getStyle("User"));
								} 
								catch (BadLocationException e2) {
									e2.printStackTrace();
								}
								displayJTextPane.setCaretPosition(doc.getLength());  // Scroll to bottom
								try {
									String clientName = null;
									if (currentContactPKH != null) {
										clientName = currentContactPKH;
										String cMessage = RSA.encryptMessage(message, new BigInteger(contactsPkDir.get(clientName).split(":")[0]), new BigInteger(contactsPkDir.get(clientName).split(":")[1]), Constants.keySizeBits);
										String cClientName = RSA.encryptMessage(clientName, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);
										String cUSER = RSA.encryptMessage(MessageTypes.USER, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);

										///////////////////////
										//
										// sign the message hash
										//
										MessageDigest md = MessageDigest.getInstance("SHA-256");
										byte[] hashInBytes = md.digest(message.getBytes("UTF-8"));
										//
										// bytes to hex
										//
										StringBuilder sb = new StringBuilder();
										for (byte b : hashInBytes) {
											sb.append(String.format("%02x", b));
										}
										String hash = sb.toString();
										String signature = RSA.encryptMessage(hash, clientKeySet.n, clientKeySet.d, Constants.keySizeBits);
										//
										///////////////////////
										dos.writeUTF(cUSER + ":" + cClientName + ":" + cMessage + ":" + signature);
										dos.flush();
									}							
								} 
								catch (IOException e1) {
									e1.printStackTrace();
								} 
								catch (NoSuchAlgorithmException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
					disconnected = true;
					list.repaint(10);
					inputJTextPane.setEditable(false);
				}
				//
				//////////////////////////////////////
				displayAllThreads();
			}

			public void keyReleased(KeyEvent e) {}

			private void appendToFile(File f, String s) {
				if (!f.exists()) {
					try {
						f.createNewFile();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				Writer out = null;
				try {
					out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8"));
					out.append(s);
				} 
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				finally {
					if (out != null) {
						try {
							out.close();
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Add a drop handler to the input text pane, to facilitate file transfer.
		//
		inputJTextPane.setDropTarget(new DropTarget() {
			@SuppressWarnings("unchecked")
			public synchronized void drop(DropTargetDropEvent evt) {
				if (disconnected) {
					list.repaint(10);
					return;
				}
				evt.acceptDrop(DnDConstants.ACTION_COPY);
				List<File> droppedFiles = null;
				try {
					droppedFiles = (List<File>)evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
				} 
				catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}

				class FTT extends Thread {
					private List<File> droppedFiles;

					public FTT(List<File> droppedFiles) {
						this.droppedFiles = droppedFiles;
					}

					public void run() {
						try {
							for (File file : droppedFiles) {
								if (disconnected) {
									list.repaint(10);
									return;
								}
								long encryptedFileLengthBytes = ((long)Math.ceil((double)file.length() / (double)(Constants.keySizeBits / 8 - 2))) * (Constants.keySizeBits / 8 + 1);

								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String message = publicKeyHashID + ":" + file.getName() + ": |" + sdf.format(timestamp) + "|:" + encryptedFileLengthBytes;
								String displayMessage = publicKeyHashID + ": " + sdf.format(timestamp) + ":" + " " + "***" + " " + "FILE TRANSFER" + " " + "[" + file.getName() + "]" + " " + "***";
								String displayMessage2 = publicKeyHashIDAliasMap.get(publicKeyHashID) + ": " + sdf.format(timestamp) + ":" + " " + "***" + " " + "FILE TRANSFER" + " " + "[" + file.getName() + "]" + " " + "***" + "\n";

								//////////////////////////////////////////
								//
								try {
									String clientPKH = null;
									if (currentContactPKH != null) {
										clientPKH = currentContactPKH;
										String cMessage = RSA.encryptMessage(message, new BigInteger(contactsPkDir.get(clientPKH).split(":")[0]), new BigInteger(contactsPkDir.get(clientPKH).split(":")[1]), Constants.keySizeBits);
										String cClientName = RSA.encryptMessage(clientPKH, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);
										String cFILE = RSA.encryptMessage(MessageTypes.FILE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits);

										///////////////////////
										//
										// sign the message hash
										//
										MessageDigest md = MessageDigest.getInstance("SHA-256");
										byte[] hashInBytes = md.digest(displayMessage.getBytes("UTF-8"));
										//
										// bytes to hex
										//
										StringBuilder sb = new StringBuilder();
										for (byte b : hashInBytes) {
											sb.append(String.format("%02x", b));
										}
										String hash = sb.toString();
										String signature = RSA.encryptMessage(hash, clientKeySet.n, clientKeySet.d, Constants.keySizeBits);
										//
										///////////////////////

										DataOutputStream fOut = null;
										DataInputStream fIn = null;

										//////////////////////////////////////
										//									//
										// 			File connection			//
										//									//
										//////////////////////////////////////
										Socket fs = null;
										try {
											fs = new Socket(serverIPAddress, serverPort);
											fs.setSoTimeout(0);
											fOut = new DataOutputStream(fs.getOutputStream());
										} 
										catch (Exception e) {
											e.printStackTrace();
											if (fs != null) {
												try {
													fs.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fOut != null) {
												try {
													fOut.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											disconnected = true;
											continue;
										}

										//////////////////////////////////////
										//
										// Send the connection type to the server.
										//
										try {
											fOut.writeUTF(MessageTypes.FILE);
											fOut.flush();
										} 
										catch (Exception e) {
											e.printStackTrace();
											if (fs != null) {
												try {
													fs.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fOut != null) {
												try {
													fOut.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											disconnected = true;
											continue;
										}		
										//
										//////////////////////////////////////

										//////////////////////////////////////
										//
										// Send our public key to the server.
										//
										try {
											fOut.writeUTF(clientKeySet.n.toString() + ":" + clientKeySet.e.toString());
											fOut.flush();
										} 
										catch (Exception e) {
											e.printStackTrace();
											if (fs != null) {
												try {
													fs.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fOut != null) {
												try {
													fOut.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											disconnected = true;
											continue;
										}		
										//
										//////////////////////////////////////

										//////////////////////////////////////
										//
										// Receive the server's public key.
										//
										try {
											fIn = new DataInputStream(fs.getInputStream());
											String messageFromServer = fIn.readUTF();
											serverPublicKeyPartI = new BigInteger(messageFromServer.split(":")[0]);
											serverPublicKeyPartII = new BigInteger(messageFromServer.split(":")[1]);
										} 
										catch (IOException e) {
											e.printStackTrace();
											if (fs != null) {
												try {
													fs.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fOut != null) {
												try {
													fOut.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											disconnected = true;
											continue;		
										}

										//////////////////////////////////////
										//
										// Now that we have exchanged keys with the server, send a unique identifier for this file transfer to the server.
										//
										try {
											fOut.writeUTF(RSA.encryptMessage(signature + "SEND", serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits));
											fOut.flush();
										} 
										catch (Exception e) {
											e.printStackTrace();
											if (fs != null) {
												try {
													fs.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fIn != null) {
												try {
													fIn.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											if (fOut != null) {
												try {
													fOut.close();
												} 
												catch (IOException e1) {
													e1.printStackTrace();
												}
											}
											disconnected = true;
											continue;		
										}		
										//
										//////////////////////////////////////

										fOut.writeUTF(cFILE + ":" + cClientName + ":" + cMessage + "|" + encryptedFileLengthBytes + ":" + signature);
										fOut.flush();

										FileInputStream fis = null;
										try {
											fis = new FileInputStream(file);
											byte[] bytes = new byte[Constants.keySizeBits / 8 - 2];
											int bytesRead = 0;
											int totalBytesRead = 0;
											do {
												bytesRead = fis.read(bytes);
												while (bytesRead < Constants.keySizeBits / 8 - 2) {
													int b = fis.read();
													if (b == -1) {
														bytes = Arrays.copyOfRange(bytes, 0, bytesRead);
														bytesRead++;
														break;
													}
													else {
														bytes[bytesRead++] = (byte)b;
													}
												}
												totalBytesRead += bytesRead;
												byte[] cBytes = RSA.encryptBytesToBytes(bytes, new BigInteger(contactsPkDir.get(clientPKH).split(":")[0]), new BigInteger(contactsPkDir.get(clientPKH).split(":")[1]), Constants.keySizeBits);
												fOut.write(cBytes);
												fOut.flush();
											} while (totalBytesRead < file.length());

											System.out.println("done writing: totalBytesRead = " + totalBytesRead);

											synchronized (renderLock) {
												if (clientPKH.equals(currentContactPKH)) {
													StyledDocument doc = displayJTextPane.getStyledDocument();
													try {
														doc.insertString(doc.getLength(), displayMessage2, doc.getStyle("User"));
													} 
													catch (BadLocationException e2) {
														e2.printStackTrace();
													}
													displayJTextPane.setCaretPosition(doc.getLength());  // Scroll to bottom
												}
												File chatFile = new File(chatFolder.getAbsolutePath() + File.separator + clientPKH);
												appendToFile(chatFile, displayMessage + "\n");
											}
										} 
										catch (IOException ex) {
											ex.printStackTrace();
										} 
										finally {
											try {
												fis.close();
											} 
											catch (IOException ex) {
												ex.printStackTrace();
											}
										}
									}
								} 
								catch (IOException e1) {
									e1.printStackTrace();
								} 
								catch (NoSuchAlgorithmException e1) {
									e1.printStackTrace();
								}								
								//
								//////////////////////////////////////////
							} 
						}
						catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
				FTT ftt = new FTT(droppedFiles);
				ftt.start();
			}
			private void appendToFile(File f, String s) {
				if (!f.exists()) {
					try {
						f.createNewFile();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				Writer out = null;
				try {
					out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8"));
					out.append(s);
				} 
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				finally {
					if (out != null) {
						try {
							out.close();
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// When the application is closed, send a BYE message to the server, so the server can dispose of its connection to us cleanly
		// and forward the BYE message to the other connected clients so they know we are offline.
		//
		jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		jf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				try {
					dos.writeUTF(RSA.encryptMessage(MessageTypes.BYE, serverPublicKeyPartI, serverPublicKeyPartII, Constants.keySizeBits) + ":0" + ":asdf" + ":asdf");
					dos.flush();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		inputJTextPane.requestFocus();
		list.setSelectedIndex(listModel.indexOf(publicKeyHashID));
		list.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(new Object(), listModel.indexOf(publicKeyHashID), listModel.indexOf(publicKeyHashID), false));
		displayJTextPane.setCaretPosition(displayJTextPane.getDocument().getLength());

		jf.pack();

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// Center the application window on the primary display.
		//
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();

		int windowWidth = jf.getWidth();
		int windowHeight = jf.getHeight();

		jf.setLocation((screenWidth - windowWidth) >> 1, (screenHeight - windowHeight) >> 1);
		//
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		ImageIcon img = createImageIcon("dormouse2.png", "Radchat");
		jf.setIconImage(img.getImage());

		jf.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				if (e.getNewState() != Frame.ICONIFIED) {
					ImageIcon img = createImageIcon("dormouse2.png", "Radchat");
					jf.setIconImage(img.getImage());
				}
			}
		});
		jf.setVisible(true);		
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected static ImageIcon createImageIcon(String path,	String description) {
		File f = new File(imagesFolder.getAbsolutePath() + File.separator + path);

		if (f.exists()) {
			return new ImageIcon(f.getAbsolutePath(), description);
		} 
		else {
			System.err.println("Couldn't find file: " + f.getAbsolutePath());
			return null;
		}
	}
}

class MessageListener extends Thread {
	public MessageListener() {}

	public void run() {
		while (!MultiThreadedClient.disconnected) {
			try {
				String messageFromServer = null;
				while (messageFromServer == null) {
					messageFromServer = MultiThreadedClient.dis.readUTF();
				}

				class ProcessorThread extends Thread {
					private String msg = null;

					public ProcessorThread(String msg) {
						this.msg = msg;
					}

					public void run() {
						if (processMessage(msg)) {
							MultiThreadedClient.disconnected = true;
							MultiThreadedClient.list.repaint(10);
							MultiThreadedClient.inputJTextPane.setEditable(false);
							return;
						}
					}
				}
				ProcessorThread pt = new ProcessorThread(messageFromServer);
				pt.start();
				//pt.join();
			}
			catch (Exception e) {
				MultiThreadedClient.disconnected = true;
				MultiThreadedClient.list.repaint(10);
				MultiThreadedClient.inputJTextPane.setEditable(false);
				break;
			}
		}		
		MultiThreadedClient.inputJTextPane.setEditable(false);
		MultiThreadedClient.disconnected = true;
		MultiThreadedClient.list.repaint(10);
	}

	private boolean processMessage(String messageFromServer) {
		String msgType = "";
		String body = "";
		String signature = "";
		String signatureVerification = "FAILED";

		MultiThreadedClient.timeOfMostRecentServerIOoperationEpochMillis = System.currentTimeMillis();

		if (messageFromServer.equals(MessageTypes.PULSE)) {
			return false;
		}

		msgType = RSA.decryptMessage(messageFromServer.split(":")[0], MultiThreadedClient.clientKeySet.d, MultiThreadedClient.clientKeySet.n);

		if (!msgType.equals(MessageTypes.FILE)) body = RSA.decryptMessage(messageFromServer.split(":")[1], MultiThreadedClient.clientKeySet.d, MultiThreadedClient.clientKeySet.n);
		if (body == null || body.contains(MultiThreadedClient.ACK) || body.contains(MultiThreadedClient.SIG) || body.contains(MultiThreadedClient.INVSIG)) {
			return false;  // Suspicious message
		}							
		signature = messageFromServer.split(":")[2];

		//////////////////////////////////////////
		// Process the message from the server. //
		//////////////////////////////////////////
		//
		// BYE
		//
		if (msgType.equals(MessageTypes.BYE)) {
			String keyMessage[] = body.split(":");
			for (int i = 0; i < MultiThreadedClient.listModel.size(); i++) {
				if (((ListItem)MultiThreadedClient.listModel.elementAt(i)).getPublicKeyHashID().equals(keyMessage[0])) {
					((ListItem)MultiThreadedClient.listModel.elementAt(i)).setLoggedIn(((ListItem)MultiThreadedClient.listModel.elementAt(i)).getLoggedIn() - 1);
					if (MultiThreadedClient.pkDir.containsValue(keyMessage[1] + ":" + keyMessage[2])) {
						if (((ListItem)MultiThreadedClient.listModel.elementAt(i)).getLoggedIn() < 1)
							MultiThreadedClient.pkDir.remove(keyMessage[1] + ":" + keyMessage[2]);
					}
				}
			}
			MultiThreadedClient.list.repaint(10);

			String senderName = body.split(":")[0];

			if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
				return false;  // unknown or invalid sender name
			}
		}
		//
		// ACK
		//
		else if (msgType.equals(MessageTypes.ACK)) {
			synchronized (MultiThreadedClient.renderLock) {
				String ackText = body.split("\"", 2)[1];
				int ackTextLength = ackText.length();
				ackText = ackText.substring(0, ackTextLength - 1);
				ackText = ackText.replaceAll("\r\n", "\n") ;
				String senderName = body.split(":")[0];

				if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
					return false;  // unknown or invalid sender name
				}

				///////
				//
				// Read in the chat file.
				//
				File chatFile = new File(MultiThreadedClient.chatFolder.getAbsolutePath() + File.separator + senderName);
				String chatText = "";

				FileInputStream fis = null;
				InputStreamReader isr = null;
				BufferedReader in = null;
				StringBuilder sb = new StringBuilder();
				try {
					fis = new FileInputStream(chatFile);
					isr = new InputStreamReader(fis, "UTF-8");
					in = new BufferedReader(isr);
					String line;
					while ((line = in.readLine()) != null) {
						sb.append(line + "\n");
					}
				} 
				catch (UnsupportedEncodingException e) 
				{
					System.out.println(e.getMessage());
				} 
				catch (IOException e) 
				{
					System.out.println(e.getMessage());
				}
				catch (Exception e)
				{
					System.out.println(e.getMessage());
				}
				finally {
					if (in != null) {
						try {
							in.close();
							in = null;
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (isr != null) {
						try {
							isr.close();
							isr = null;
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (fis != null) {
						try {
							fis.close();
							fis = null;
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				//
				///////
				chatText = sb.toString();
				String chatTextAcked = chatText.replace(ackText, ackText + " [" + body.split(":")[0] + " " + MultiThreadedClient.ACK + "]");				
				overwriteFile(chatFile, chatTextAcked);
				chatTextAcked = null;
				chatText = null;
				chatFile = null;

				if (senderName.equals(MultiThreadedClient.currentContactPKH)) {
					MultiThreadedClient.list.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(this, 0, MultiThreadedClient.listModel.getSize() - 1, false));
				}
			}
		}
		//
		// KEY
		//
		else if (msgType.equals(MessageTypes.KEY)) {
			String keyMessage[] = body.split(":");
			if (!MultiThreadedClient.pkDir.containsValue(keyMessage[1] + ":" + keyMessage[2])) {
				MultiThreadedClient.pkDir.put(keyMessage[0], keyMessage[1] + ":" + keyMessage[2]);
			}
			for (int i = 0; i < MultiThreadedClient.listModel.size(); i++) {
				if (((ListItem)MultiThreadedClient.listModel.elementAt(i)).getPublicKeyHashID().equals(keyMessage[0])) {
					((ListItem)MultiThreadedClient.listModel.elementAt(i)).setLoggedIn(((ListItem)MultiThreadedClient.listModel.elementAt(i)).getLoggedIn() + 1);
				}
			}
			MultiThreadedClient.list.repaint(10);

			String senderName = body.split(":")[0];

			if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
				return false;  // unknown or invalid sender name
			}
		}
		//
		// USER
		//
		else if (msgType.equals(MessageTypes.USER)) { 
			////
			//
			// Verify the digital signature.
			//
			String senderName = body.split(":")[0];

			if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
				return false;  // unknown or invalid sender name
			}

			String hash = "";
			try {
				hash = RSA.decryptMessage(signature, new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[1]), new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[0]));
			}
			catch (Exception e) {
				return false;
			}
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} 
			catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			byte[] hashInBytes = null;
			try {
				hashInBytes = md.digest(body.getBytes("UTF-8"));
			} 
			catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			//
			// bytes to hex
			//
			StringBuilder sb = new StringBuilder();
			for (byte b : hashInBytes) {
				sb.append(String.format("%02x", b));
			}
			String hashedBody = sb.toString();

			signatureVerification = "FAILED";
			if (hash.equals(hashedBody)) {
				signatureVerification = "SUCCEEDED";
			}
			//
			////	
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String ack = MultiThreadedClient.publicKeyHashID + ": " + MultiThreadedClient.sdf.format(timestamp) + ": " + "ACK" + ": \"" + body + "\"";
			String clientName = senderName;
			String cAck = RSA.encryptMessage(ack, new BigInteger(MultiThreadedClient.contactsPkDir.get(clientName).split(":")[0]), new BigInteger(MultiThreadedClient.contactsPkDir.get(clientName).split(":")[1]), Constants.keySizeBits);
			String cClientName = RSA.encryptMessage(clientName, MultiThreadedClient.serverPublicKeyPartI, MultiThreadedClient.serverPublicKeyPartII, Constants.keySizeBits);
			String cACK = RSA.encryptMessage(MessageTypes.ACK, MultiThreadedClient.serverPublicKeyPartI, MultiThreadedClient.serverPublicKeyPartII, Constants.keySizeBits);
			try {
				MultiThreadedClient.dos.writeUTF(cACK + ":" + cClientName + ":" + cAck + ":asdf");
				MultiThreadedClient.dos.flush();
			}
			catch (Exception e) {
				e.printStackTrace();
				MultiThreadedClient.disconnected = true;
				MultiThreadedClient.list.repaint(10);
				MultiThreadedClient.inputJTextPane.setEditable(false);
				return true;
			}
			synchronized (MultiThreadedClient.renderLock) {
				File chatFile = new File(MultiThreadedClient.chatFolder.getAbsolutePath() + File.separator + senderName);

				String sigVerText = signatureVerification.equals("SUCCEEDED") ? MultiThreadedClient.SIG + " " + senderName : MultiThreadedClient.INVSIG;
				String textToAppend = body.replaceAll("\r\n", "\n") + "(" + sigVerText + ")";

				appendToFile(chatFile, textToAppend + "\n");
				MultiThreadedClient.list.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(this, 0, MultiThreadedClient.listModel.getSize() - 1, false));

				if (MultiThreadedClient.jf.getState() == Frame.ICONIFIED) {
					ImageIcon img = createImageIcon("new_message.png", "Radchat msg");
					MultiThreadedClient.jf.setIconImage(img.getImage());
					try {
						playSound("messageReceived.wav");
					} 
					catch (MalformedURLException e) {
						e.printStackTrace();
					} 
					catch (LineUnavailableException e) {
						e.printStackTrace();
					} 
					catch (UnsupportedAudioFileException e) {
						e.printStackTrace();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (!senderName.equals(MultiThreadedClient.currentContactPKH)) {
					for (int i = 0; i < MultiThreadedClient.listModel.size(); i++) {
						if (((ListItem)MultiThreadedClient.listModel.elementAt(i)).getPublicKeyHashID().equals(senderName)) {
							((ListItem)MultiThreadedClient.listModel.elementAt(i)).setNewMessage(true);
						}
					}
					MultiThreadedClient.list.repaint(10);
				}
			}
		}
		//
		// TYPING
		//
		else if (msgType.equals(MessageTypes.TYPING)) {
			////
			//
			// Verify the digital signature.
			//
			String senderName = body.split(":")[0];

			if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
				return false;  // unknown or invalid sender name
			}

			String hash = "";
			try {
				hash = RSA.decryptMessage(signature, new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[1]), new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[0]));
			}
			catch (Exception e) {
				return false;
			}
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} 
			catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			byte[] hashInBytes = null;
			try {
				hashInBytes = md.digest(body.getBytes("UTF-8"));
			} 
			catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			//
			// bytes to hex
			//
			StringBuilder sb = new StringBuilder();
			for (byte b : hashInBytes) {
				sb.append(String.format("%02x", b));
			}
			String hashedBody = sb.toString();

			signatureVerification = "FAILED";
			if (hash.equals(hashedBody)) {
				signatureVerification = "SUCCEEDED";
			}
			//
			////				
			if (senderName.equals(MultiThreadedClient.currentContactPKH)) {
				Thread typingNotificationThread = new Thread() {
					public void run(){
						MultiThreadedClient.jf.setTitle("Radchat" + " " + MultiThreadedClient.VERSION + " " + "(" + MultiThreadedClient.alias + ")" + "   " + "[" + MultiThreadedClient.publicKeyHashIDAliasMap.get(MultiThreadedClient.currentContactPKH) + " is typing..." + "]");
						try {
							Thread.sleep(5000);
						} 
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						finally {
							MultiThreadedClient.jf.setTitle("Radchat" + " " + MultiThreadedClient.VERSION + " " + "(" + MultiThreadedClient.alias + ")" + "   " + "[" + "Chatting with " + MultiThreadedClient.publicKeyHashIDAliasMap.get(MultiThreadedClient.currentContactPKH) + "]");
						}
					}
				};
				typingNotificationThread.start();
			}
		}
		//
		// FILE
		//
		else if (msgType.equals(MessageTypes.FILE)) { 
			////
			//
			// Verify the digital signature.
			//
			body = RSA.decryptMessage(messageFromServer.split(":")[1].split("\\|")[0], MultiThreadedClient.clientKeySet.d, MultiThreadedClient.clientKeySet.n);
			String senderName = body.split(":")[0];

			String[] bodySplit = body.split(":");
			long encryptedFileLengthBytes = Long.parseLong(bodySplit[bodySplit.length - 1]);

			System.out.println("encryptedFileLengthBytes=" + encryptedFileLengthBytes);

			String fileName = bodySplit[1];

			String displayMessage = senderName + ": " + body.split("\\|")[1] + ":" + " " + "***" + " " + "FILE TRANSFER" + " " + "[" + fileName + "]" + " " + "***";

			if (!MultiThreadedClient.publicKeyHashIDAliasMap.containsKey(senderName)) {
				return false;  // unknown or invalid sender name
			}

			String hash = "";
			try {
				hash = RSA.decryptMessage(signature, new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[1]), new BigInteger(MultiThreadedClient.contactsPkDir.get(senderName).split(":")[0]));
			}
			catch (Exception e) {
				return false;
			}
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} 
			catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			byte[] hashInBytes = null;
			try {
				hashInBytes = md.digest(displayMessage.getBytes("UTF-8"));
			} 
			catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			//
			// bytes to hex
			//
			StringBuilder sb = new StringBuilder();
			for (byte b : hashInBytes) {
				sb.append(String.format("%02x", b));
			}
			String hashedBody = sb.toString();

			signatureVerification = "FAILED";
			if (hash.equals(hashedBody)) {
				System.out.println("hashes are equal");
				signatureVerification = "SUCCEEDED";

				DataOutputStream fOut = null;
				DataInputStream fIn = null;
				//////////////////////////////////////
				//									//
				// 			File connection			//
				//									//
				//////////////////////////////////////
				Socket fs = null;
				try {
					fs = new Socket(MultiThreadedClient.serverIPAddress, MultiThreadedClient.serverPort);
					fs.setSoTimeout(0);
					fOut = new DataOutputStream(fs.getOutputStream());
				} 
				catch (Exception e) {
					e.printStackTrace();
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					return true;
				}

				//////////////////////////////////////
				//
				// Send the connection type to the server.
				//
				try {
					fOut.writeUTF(MessageTypes.FILE);
					fOut.flush();
				} 
				catch (Exception e) {
					e.printStackTrace();
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					return true;					
				}		
				//
				//////////////////////////////////////

				//////////////////////////////////////
				//
				// Send our public key to the server.
				//
				try {
					fOut.writeUTF(MultiThreadedClient.clientKeySet.n.toString() + ":" + MultiThreadedClient.clientKeySet.e.toString());
					fOut.flush();
				} 
				catch (Exception e) {
					e.printStackTrace();
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					return true;
				}		
				//
				//////////////////////////////////////

				//////////////////////////////////////
				//
				// Receive the server's public key.
				//
				try {
					fIn = new DataInputStream(fs.getInputStream());
					String messageFromServer2 = null;
					messageFromServer2 = fIn.readUTF();
					MultiThreadedClient.serverPublicKeyPartI = new BigInteger(messageFromServer2.split(":")[0]);
					MultiThreadedClient.serverPublicKeyPartII = new BigInteger(messageFromServer2.split(":")[1]);
				} 
				catch (IOException e) {
					e.printStackTrace();
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					return true;
				}

				//////////////////////////////////////
				//
				// Now that we have exchanged keys with the server, send a unique identifier for this file transfer to the server.
				//
				try {
					fOut.writeUTF(RSA.encryptMessage(signature + "RCV", MultiThreadedClient.serverPublicKeyPartI, MultiThreadedClient.serverPublicKeyPartII, Constants.keySizeBits));
					fOut.flush();
				} 
				catch (Exception e) {
					e.printStackTrace();
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fIn != null) {
						try {
							fIn.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					return true;		
				}		
				//
				//////////////////////////////////////

				File rcvFile = new File(MultiThreadedClient.rcvFolder.getAbsolutePath() + File.separator + fileName + "_" + System.currentTimeMillis());
				FileOutputStream outputStream = null;
				signatureVerification = "SUCCEEDED";

				try {
					outputStream = new FileOutputStream(rcvFile, false);
					byte[] cBytes = new byte[Constants.keySizeBits / 8 + 1];
					byte[] bytes;
					int bytesRead = 0;
					long totalBytesRead = 0;
					try {
						do {
							bytesRead = fIn.read(cBytes);
							while (bytesRead < Constants.keySizeBits / 8 + 1) {
								int b = fIn.read();
								if (b == -1) {
									throw new Exception("End of stream encountered while reading block.");
								}
								else {
									cBytes[bytesRead++] = (byte)b;
								}
							}
							totalBytesRead += bytesRead;
							bytes = RSA.decryptBytesToBytes(cBytes, MultiThreadedClient.clientKeySet.d, MultiThreadedClient.clientKeySet.n, Constants.keySizeBits);
							outputStream.write(bytes);
							Thread.sleep(10);
						} while (totalBytesRead < encryptedFileLengthBytes);
						System.out.println("done reading: totalBytesRead = " + totalBytesRead);
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				} 
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				finally {
					if (outputStream != null)
						try {
							outputStream.close();
						} 
					catch (IOException e) {
						e.printStackTrace();
					}
					if (fs != null) {
						try {
							fs.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fIn != null) {
						try {
							fIn.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					if (fOut != null) {
						try {
							fOut.close();
						} 
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			//
			////				
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String ack = MultiThreadedClient.publicKeyHashID + ": " + MultiThreadedClient.sdf.format(timestamp) + ": " + "ACK" + ": \"" + displayMessage + "\"";
			String clientName = senderName;
			String cAck = RSA.encryptMessage(ack, new BigInteger(MultiThreadedClient.contactsPkDir.get(clientName).split(":")[0]), new BigInteger(MultiThreadedClient.contactsPkDir.get(clientName).split(":")[1]), Constants.keySizeBits);
			String cClientName = RSA.encryptMessage(clientName, MultiThreadedClient.serverPublicKeyPartI, MultiThreadedClient.serverPublicKeyPartII, Constants.keySizeBits);
			String cACK = RSA.encryptMessage(MessageTypes.ACK, MultiThreadedClient.serverPublicKeyPartI, MultiThreadedClient.serverPublicKeyPartII, Constants.keySizeBits);
			try {
				MultiThreadedClient.dos.writeUTF(cACK + ":" + cClientName + ":" + cAck + ":asdf");
				MultiThreadedClient.dos.flush();
			}
			catch (Exception e) {
				e.printStackTrace();
				MultiThreadedClient.disconnected = true;
				MultiThreadedClient.list.repaint(10);
				MultiThreadedClient.inputJTextPane.setEditable(false);
				return true;
			}
			synchronized (MultiThreadedClient.renderLock) {
				File chatFile = new File(MultiThreadedClient.chatFolder.getAbsolutePath() + File.separator + senderName);

				String sigVerText = signatureVerification.equals("SUCCEEDED") ? MultiThreadedClient.SIG + " " + senderName : MultiThreadedClient.INVSIG;
				String textToAppend = displayMessage.replaceAll("\r\n", "\n") + "(" + sigVerText + ")";

				appendToFile(chatFile, textToAppend + "\n");
				MultiThreadedClient.list.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(this, 0, MultiThreadedClient.listModel.getSize() - 1, false));

				if (MultiThreadedClient.jf.getState() == Frame.ICONIFIED) {
					ImageIcon img = createImageIcon("new_message.png", "Radchat msg");
					MultiThreadedClient.jf.setIconImage(img.getImage());
					try {
						playSound("messageReceived.wav");
					} 
					catch (MalformedURLException e) {
						e.printStackTrace();
					} 
					catch (LineUnavailableException e) {
						e.printStackTrace();
					} 
					catch (UnsupportedAudioFileException e) {
						e.printStackTrace();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (!senderName.equals(MultiThreadedClient.currentContactPKH)) {
					for (int i = 0; i < MultiThreadedClient.listModel.size(); i++) {
						if (((ListItem)MultiThreadedClient.listModel.elementAt(i)).getPublicKeyHashID().equals(senderName)) {
							((ListItem)MultiThreadedClient.listModel.elementAt(i)).setNewMessage(true);
						}
					}
					MultiThreadedClient.list.repaint(10);
				}
			}
		}
		return false;
	}

	private void appendToFile(File f, String s) {
		if (!f.exists()) {
			try {
				f.createNewFile();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8"));
			out.append(s);
		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void overwriteFile(File f, String s) {
		if (!f.exists()) {
			try {
				f.createNewFile();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
			out.append(s);
		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void playSound(String soundFile) throws LineUnavailableException, MalformedURLException, UnsupportedAudioFileException, IOException {
		File f = new File(MultiThreadedClient.soundFolder.getAbsolutePath() + File.separator + soundFile);
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());  
		Clip clip = AudioSystem.getClip();
		clip.open(audioIn);
		clip.start();
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected static ImageIcon createImageIcon(String path,	String description) {
		File f = new File(MultiThreadedClient.imagesFolder.getAbsolutePath() + File.separator + path);

		if (f.exists()) {
			return new ImageIcon(f.getAbsolutePath(), description);
		} 
		else {
			System.err.println("Couldn't find file: " + f.getAbsolutePath());
			return null;
		}
	}
}

class ListItem {
	private String alias;
	private String publicKeyHashID;
	private int loggedIn = 0;
	private boolean newMessage = false;

	public ListItem(String alias, String publicKeyHashID) {		
		this.alias = alias;
		this.publicKeyHashID = publicKeyHashID;
	}

	public void setNewMessage(boolean newMessage) {
		this.newMessage = newMessage;		
	}

	public String getDisplayText() {
		return alias + (newMessage  ? " \u2709" : "      ");   // 2709 should be an envelope
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getPublicKeyHashID() {
		return publicKeyHashID;
	}

	public void setPublicKeyHashID(String publicKeyHashID) {
		this.publicKeyHashID = publicKeyHashID;
	}

	public int getLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(int loggedIn) {
		if (loggedIn < 0) loggedIn = 0;
		this.loggedIn = loggedIn;
	}
}