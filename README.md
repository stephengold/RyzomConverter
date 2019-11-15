RyzomConverter converts models from the Ryzom Asset Repository
for use with [jMonkeyEngine](http://jmonkeyengine.org).

RyzomConverter derives from Java software created by Alweth.
Before using, modifying, selling, or distributing
this software or its output, please read
[Alweth's license](https://github.com/stephengold/RyzomConverter/blob/master/LICENSE)
very carefully.

The Ryzom Asset Repository is available under a
[CC-BY-SA 3.0 license](https://creativecommons.org/licenses/by-sa/3.0/),
so if you distribute the converted files or anything built from them,
you should do so under that license.

The suggested procedure is:

  1. mkdir RyzomAssets
  2. cd RyzomAssets
  3. git clone https://bitbucket.org/ccxvii/ryzom-assets.git
  4. cd ..
  5. git clone https://github.com/stephengold/RyzomConverter.git
  6. cd RyzomConverter
  7. ./gradlew run

Converted files are written to the assets/ryzom-assets/export folder.
For more information, see
[the relevant topic at the jME Forum](https://hub.jmonkeyengine.org/t/convert-all-ryzom-character-models-and-animations-to-j3o-format/37859).

