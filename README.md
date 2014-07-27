Sesame
======

Sesame is yet another Android password keyring. Unlike many implementations, Sesame is both open source and moderately secure.

Extremely alpha release: https://drive.google.com/file/d/0B5PckidoLJaecHlfMmxyVFlGV1U.

# Why?

Most Android password managers are [insecure](https://media.blackhat.com/bh-eu-12/Belenko/bh-eu-12-Belenko-Password_Encryption-Slides.pdf). 

Perhaps the best one out there, from a brief survey, is Zetetic's [STRIP](https://play.google.com/store/apps/details?id=net.zetetic.strip). 

These guys develop the awesome (and open source) SQLCipher library, which is used in Sesame, but STRIP itself uses a relatively small number of key derivation rounds before encrypting the database, and demands some additional app permissions in order to provide useful features like database export. 

In contrast, Sesame uses a rather ungainly number of PBKDF rounds--which means performance on older devices is truly horrid--and demands no special permissions. 

On the other hand, I owe a rather large debt of gratitude to Zetetic for developing SQLCipher, so if you like Sesame, go buy STRIP. 

# How?

Sesame implements no encryption itself. Storage is provided by [SQLCipher](http://sqlcipher.net/design), which seems to mostly do the right thing in all the usual places. Sesame is just a thin layer on top of that. 

Note that I have not actually reviewed the SQLCipher code. I trust that these guys mostly did things correctly, and if they did not I wouldn't necessarily spot it. ;) 

# Extra features

In addition, Sesame provides automatic cloud backup to the Android servers (but you can turn this off if you like) and manual database import/export (using Android Intents, so no additional file permissions are required). 
