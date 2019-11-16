RyzomConverter adapts character models and animations
from the Ryzom Asset Repository
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

The suggested procedure (for Bash users) is:

 1. `mkdir RyzomAssets`
 2. `cd RyzomAssets`
 3. `git clone https://bitbucket.org/ccxvii/ryzom-assets.git`
 4. `cd ..`
 5. `git clone https://github.com/stephengold/RyzomConverter.git`
 6. `cd RyzomConverter`
 7. `./gradlew run`

Converted assets are written to the
`RyzomConverter/assets/ryzom-assets/export` folder.
They should occupy about 500 MBytes of filesystem storage.
There should be 4 J3O animations files,
697 J3O geometry files, and 1183 PNG texture files.
Once the converter has successfully run to completion,
the RyzomAssets folder
(which should occupy about 3.5 GBytes of storage) may be deleted.

The converted assets occupy less storage space
because not all assets in the Ryzom Asset Repository get converted:
only assets in the "actors" folder, and even there, the
"banners", "tools", and "weapons" sub-folders are skipped.

To construct a character in Java code:

 1. register a locator:
    + `assetManager.registerLocator("../RyzomConverter/assets", FileLocator.class);`
 2. load an animations asset and attach it to the scene graph:
    + `ModelKey key = new ModelKey("ryzom-assets/export/animations_ge_hom.j3o");`
    + `Node characterNode = (Node) assetManager.loadAsset(key);`
    + `rootNode.attachChild(characterNode)`
 3. load a geometry asset for each body part and attach it to the character node:

    key = new ModelKey("ryzom-assets/export/fy_hom_armor01_armpad.j3o");
    Spatial arms = manager.loadAsset(key);
    characterNode.attachChild(arms);

    key = new ModelKey("ryzom-assets/export/fy_hom_armor01_gilet.j3o");
    Spatial chest = manager.loadAsset(key);
    characterNode.attachChild(chest);

    key = new ModelKey("ryzom-assets/export/fy_hom_visage.j3o");
    Spatial face = manager.loadAsset(key);
    characterNode.attachChild(face);

    key = new ModelKey("ryzom-assets/export/fy_hom_armor01_bottes.j3o");
    Spatial feet = manager.loadAsset(key);
    characterNode.attachChild(feet);

    key = new ModelKey("ryzom-assets/export/fy_hom_cheveux_basic01.j3o");
    Spatial hair = manager.loadAsset(key);
    characterNode.attachChild(hair);

    key = new ModelKey("ryzom-assets/export/fy_hom_armor01_hand.j3o");
    Spatial hands = manager.loadAsset(key);
    characterNode.attachChild(hands);

    key = new ModelKey("ryzom-assets/export/fy_hom_armor01_pantabottes.j3o");
    Spatial legs = manager.loadAsset(key);
    characterNode.attachChild(legs);

Each geometry is designed for a specific skeletal group, either `ca` or `ge`.
Geometries returned by `loadAsset()` are for the `ca` skeletal group.
Geometries for the `ge` skeletal group
are stashed in the `ryzom_alternative` user data of each geometry asset.

 + The `ca` group provides 76 animations for males and 53 for females.
 + The `ge` group provides 1654 animations for males and 1452 for females.

For more information, read
[the relevant topic at the jME Forum](https://hub.jmonkeyengine.org/t/convert-all-ryzom-character-models-and-animations-to-j3o-format/37859).