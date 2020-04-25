# radchat
![dormouse](https://github.com/justinddix/radchat/blob/master/src/images/dormouse2.png) 

## **A Cryptographic Chat Application for Windows, Mac, and Linux**

This repository contains the complete Java source code for the client.  The server code is not publically available.

You are free to use the client and distribute it in source or binary form with or without modification.  You can use it with the default server.

If you would like a private server, you can request one, although there is a hosting fee for that.

In any case, encryption is end-to-end, so the server sees only encrypted data that it cannot decipher.

The application uses 4096-bit RSA encryption for messages, and for file transfer.  File transfer is enabled by drag and drop into the input box.

All messages are digitally signed using the sender's private key.

On the first run of the client, keyset/public.txt and keyset/private.txt are generated.  To chat with someone, exchange public.txt files with them.  Keep private.txt secret.  Place all of your contacts' public.txt files in your contacts/ folder.  You can rename them to anything you want.  Only the files in keyset/ must be named public.txt and private.txt.

To define a local alias for yourself or for a contact, edit the first line of the keyset/public.txt or contacts/ file to change the hash before the first : to the desired alias.  This alias is for your local display and will not be transmitted over the network or affect the display on other cients.

If you mess up your key files somehow, you can delete them and restart the client to generate a new set.

![dormouse](https://github.com/justinddix/radchat/blob/master/src/images/demo.png) 

If you have any questions, feature requests, or would like a private server, you may contact me at justin.d.dix@startmail.com
