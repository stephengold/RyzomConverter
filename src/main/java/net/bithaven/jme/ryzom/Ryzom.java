package net.bithaven.jme.ryzom;

import static java.util.logging.Level.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;

import net.bithaven.jme.AnimationRetargeter;

import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;

/**
 * Copyright Alweth on hub.jmonkeyengine.org forums 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software (the "Software"), to use, and modify the Software subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * The software or any of its derivatives shall not be sold or distributed
 * independently of the expressed intention of the author.
 *
 * Any distribution of any of the output of the Software or its derivatives, or
 * any derivative of such output shall include with it acknowledgement of the
 * contribution of the author of this software, as above, in providing this
 * software for use, free of charge.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author Alweth on hub.jmonkeyengine.org forums
 *
 */
public class Ryzom {

    private static final Logger logger = Logger.getLogger(Ryzom.class.getName());
    public static String ryzomAssets = "ryzom-assets";
    private static HashMap<String, Spatial> animationAtlas = new HashMap<String, Spatial>();
    private static HashMap<String, Skeleton> skeletonAtlas = new HashMap<String, Skeleton>();

    private Ryzom() {
    }

    ;

	public static void main(String[] args) {

    }

    private static Spatial loadAnim(AssetManager assetManager, String animationName) {
        Spatial animation = animationAtlas.get(animationName);
        if (animation == null) {
            animation = assetManager.loadModel(ryzomAssets + "/actors/anims/" + animationName + ".iqe");
            AnimControl ac2 = animation.getControl(AnimControl.class);
            Animation a = ac2.getAnim(animationName);
            Skeleton sk = Ryzom.getSkeleton(assetManager, extractSkeletonCode(animationName));
            AnimationRetargeter.retarget(a, ac2.getSkeleton(), sk);
            animation.removeControl(ac2);
            animation.removeControl(SkeletonControl.class);
            AnimControl ac3 = new AnimControl(sk);
            ac3.addAnim(a);
            animation.addControl(ac3);
            animation.addControl(new SkeletonControl(sk));

            animationAtlas.put(animationName, animation);
        }
        return animation;
    }

    public static void addAnim(AssetManager assetManager, AnimControl animControl, String animationName) {
        animControl.addAnim(loadAnim(assetManager, animationName).getControl(AnimControl.class).getAnim(animationName));
    }

    public static void addAnim(AssetManager assetManager, AnimControl animControl, String animationName, String code) {
        Spatial animation = animationAtlas.get(animationName + ":" + code);
        if (animation == null) {
            Spatial old = loadAnim(assetManager, animationName);
            animation = new Node(animationName + ":" + code);
            AnimControl ac2 = old.getControl(AnimControl.class);
            Animation a = ac2.getAnim(animationName).clone();
            Skeleton sk = Ryzom.getSkeleton(assetManager, code);
            AnimationRetargeter.retarget(a, ac2.getSkeleton(), sk);
            AnimControl ac3 = new AnimControl(sk);
            ac3.addAnim(a);
            animation.addControl(ac3);
            animation.addControl(new SkeletonControl(sk));

            animationAtlas.put(animationName + ":" + code, animation);
        }
        animControl.addAnim(animation.getControl(AnimControl.class).getAnim(animationName));
    }

    public static void fixSkeleton(AssetManager assetManager, Spatial toFix) {
        String name = toFix.getName();
        if (name == null) {
            return;
        }
        String prefix = extractSkeletonCode(name);

        AnimationRetargeter.retarget(toFix, getSkeleton(assetManager, prefix));
    }

    private static Pattern skeletonCodePattern = Pattern.compile("(ca|fy|ma|tr|zo|ge)_ho[fm]");
    private static Pattern skeletonCodePattern2 = Pattern.compile("(ca|fy|ma|tr|zo|ge).*_[fh]_");
    private static Pattern skeletonCodePattern3 = Pattern.compile("ho[fm]");

    public static @Nullable
    String extractSkeletonCode(@Nonnull String name) {
        Matcher m = skeletonCodePattern.matcher(name);
        if (!m.find()) {
            m = skeletonCodePattern2.matcher(name);
            if (!m.find()) {
                m = skeletonCodePattern3.matcher(name);
                if (!m.find()) {
                    return null;
                } else {
                    return "ge_" + m.group();
                }
            } else {
                String code = m.group();
                int c = code.charAt(code.length() - 2);
                String sex = c == 'f' ? "_hof" : "_hom";
                String race = code.substring(0, 2);
                if (!race.equals("ca")) {
                    race = "ge";
                }
                return race + sex;
            }
        } else {
            String code = m.group();
            String sex = code.substring(2);
            String race = code.substring(0, 2);
            if (!race.equals("ca")) {
                race = "ge";
            }
            return race + sex;
        }
    }

    public static final String switchSkeletonCodeRace(String code) {
        if ("ca".equals(code.substring(0, 2))) {
            return "ge" + code.substring(2);
        } else {
            return "ca" + code.substring(2);
        }
    }

    public static Skeleton getSkeleton(AssetManager assetManager, String prefix) {
        Skeleton s = skeletonAtlas.get(prefix + "_skel");
        if (s == null) {
            Spatial sModel = assetManager.loadModel(ryzomAssets + "/actors/" + prefix + "_skel.iqe");
            s = sModel.getControl(SkeletonControl.class).getSkeleton();
            skeletonAtlas.put(prefix + "_skel", s);
        }
        return s;
    }

    public static enum ModelPart {
        HAIR("cheveux", "cheveux"),
        FACE("visage", "visage"),
        ARMOR_HELMET("armor", "casque"),
        ARMOR_CHEST("armor", "gilet", "torse"),
        ARMOR_ARMPADS("armor", "armpad"),
        GAUNTLET("gauntlets", "gauntlet"),
        ARMOR_HANDS("armor", "hand"),
        ARMOR_PANTS("armor", "pantabottes", "pantabotte", "dress"),
        ARMOR_BOOTS("armor", "bottes", "botte");

        public final String dir;
        public final String[] codes;

        private ModelPart(String dir, String... codes) {
            this.dir = dir;
            this.codes = codes;
        }

        public final DirectoryStream.Filter<Path> fileFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path path) throws IOException {
                String name = path.getFileName().toString();
                if (!name.endsWith(".iqe")) {
                    return false;
                }
                if (checkModelPart(name) != null) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        public @Nullable
        String checkModelPart(@Nonnull String name) {
            for (String code : codes) {
                if (name.indexOf("_" + code) != -1) {
                    return code;
                }
            }
            return null;
        }
    }

    public static enum Sex {
        FEMALE("hof"),
        MALE("hom");

        public final String code;

        private Sex(String code) {
            this.code = code;
        }

        public boolean is(String test) {
            char c;
            if (this == MALE) {
                c = 'h';
            } else {
                c = 'f';
            }
            return test.indexOf("_" + code + "_") != -1 || test.indexOf("_" + c + "_") != -1;
        }

        public String changeTo(String s) {
            if (this == MALE) {
                return s.replaceAll("_hof_", "_hom_").replaceAll("_f_", "_h_");
            } else {
                return s.replaceAll("_hom_", "_hof_").replaceAll("_h_", "_f_");
            }
        }
    }

    public static LinkedList<Spatial> generateSpatialsFromPath(AssetManager am, Path p, Path ryzomRootPath) {
        LinkedList<Spatial> outNodes = new LinkedList<Spatial>();
        Spatial s = generateSpatialFromPath(am, p, ryzomRootPath);
        outNodes.add(s);

        HashSet<String> skins = new HashSet<String>();

        getSkinInfo(p, s, skins);

        generateAlternateRaceVersion(am, s);

        String skin = s.getUserData("ryzom_skin");
        skins.remove(skin);
        for (String skin2 : skins) {
            Spatial clone = s.clone(true);
            changeSkin(am, ryzomRootPath, p, clone, skin2);
            clone.setUserData("ryzom_skin", skin2);
            generateAlternateRaceVersion(am, clone);
            outNodes.add(clone);
        }

        logger.log(INFO, "Generated " + outNodes.size() + " skin variants.");

        return outNodes;
    }

    private static void generateAlternateRaceVersion(AssetManager am, Spatial s) {
        String code = extractSkeletonCode(s.getName());
        Spatial o = s.deepClone();
        s.setUserData("ryzom_skeleton", code);
        String code2 = switchSkeletonCodeRace(code);
        o.setUserData("ryzom_skeleton", code2);
        AnimationRetargeter.retarget(o, getSkeleton(am, code), getSkeleton(am, code2));
        s.setUserData("ryzom_alternate", o);
        o.setUserData("ryzom_alternate", s);
    }

    private static void changeSkin(AssetManager am, Path ryzomRootPath, Path p, Spatial s, String skin) {
        if (s instanceof Geometry) {
            String materialName = (String) s.getUserData("IQEMaterial");
            if (materialName != null) {
                Path dir = p.getParent().resolve("textures");
                Iterator<String> iter = materialNameVariants(materialName, skin);
                while (iter.hasNext()) {
                    String testName = iter.next();
                    if (Files.exists(dir.resolve(testName + ".png"))) {
                        s.setUserData("IQEMaterial", testName);
                        ((Geometry) s).getMaterial().setTexture("ColorMap", am.loadTexture(
                                new TextureKey(ryzomRootPath.relativize(dir).toString() + "/" + testName + ".png",
                                        false)));
                        break;
                    }
                }
                String newName = materialName;
                if (skin.charAt(0) != '-') {
                    newName = variantPattern.matcher(newName).replaceAll(skin.substring(0, 2));
                }
                if (!skin.endsWith("-")) {
                    newName = colorPattern.matcher(newName)
                            .replaceAll(skin.substring(skin.indexOf(":") + 1));
                }
            }
        }
        if (s instanceof Node) {
            for (Spatial ss : ((Node) s).getChildren()) {
                changeSkin(am, ryzomRootPath, p, ss, skin);
            }
        }
    }

    private static Pattern variantPattern = Pattern.compile("\\d\\d");
    private static Pattern colorPattern = Pattern.compile("_(c\\d|com|off)");

    private static void getSkinInfo(Path p, Spatial s, HashSet<String> skins) {
        String material = (String) s.getUserData("IQEMaterial");
        String skin = null;
        if (material != null) {
            skin = skinFromMaterialName(material);
            skins.add(skin);
            s.setUserData("ryzom_skin", skin);
            getMaterialInfo(p, material, skins);
        }

        if (s instanceof Node) {
            for (Spatial ss : ((Node) s).getChildren()) {
                getSkinInfo(p, ss, skins);
                if (skin == null) {
                    skin = (String) ss.getUserData("ryzom_skin");
                    s.setUserData("ryzom_skin", skin);
                }
            }
        }
    }

    private static void getMaterialInfo(Path path, String material, HashSet<String> skins) {
        Pattern matRegex = Pattern.compile(regexFromMaterialName(material));

        Path textureDir = path.getParent().resolve("textures");
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(textureDir);
            for (Path p : ds) {
                String name = p.getFileName().toString();
                if (matRegex.matcher(name).find()) {
                    skins.add(skinFromMaterialName(name));
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static String regexFromMaterialName(String material) {
        Matcher m = variantPattern.matcher(material);
        if (m.find()) {
            material = material.replace(m.group(), variantPattern.pattern());
        }
        m = colorPattern.matcher(material);
        if (m.find()) {
            material = material.replace(m.group(), colorPattern.pattern());
        } else {
            material += "(" + colorPattern.pattern() + ")?";
        }
        return "^" + material + "\\.png$";
    }

    public static Spatial generateSpatialFromPath(AssetManager am, Path p, Path ryzomRootPath) {
        String name = p.getFileName().toString();
        name = name.substring(0, name.length() - 4); // Remove the extension.
        Spatial s = am.loadModel(ryzomRootPath.relativize(p).toString());
        fixSkeleton(am, s);

        fixIrregularities(s);

        s.setName(name);
        String path = ryzomRootPath.relativize(p).toString();
        s.setUserData("path", path);
        ModelPart part = null;
        String code = null;

        for (ModelPart check : ModelPart.values()) {
            code = check.checkModelPart(name);
            if (code != null) {
                part = check;
                break;
            }
        }
        if (part == null) {
            return s;
        }

        s.setUserData("ryzom_part", part.toString());
        s.setUserData("ryzom_part_name", code);
        s.setUserData("ryzom_style", name.substring(0, name.indexOf(code)));
        s.setUserData("ryzom_substyle", name.substring(name.indexOf(code) + code.length()));

        return s;
    }

    //TODO: There's really no need to traverse every subspatial just to fix this one problem.
    private static void fixIrregularities(Spatial s) {
        //Hacks to fix irregularities in the Ryzom naming.
        s.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if ("ca_hof_armor_01_epaule_c1".equals((String) spatial.getUserData("IQEMaterial"))) {
                    //Make it consistent with how the other skins are named so that variants can be found.
                    spatial.setUserData("IQEMaterial", "ca_hof_armor01_epaule_c1");
                }
            }
        });
    }

    private static String skinFromMaterialName(String name) {
        String variant = "-";
        String color = "-";
        Matcher m = variantPattern.matcher(name);
        if (m.find()) {
            variant = m.group();
        }
        m = colorPattern.matcher(name);
        if (m.find()) {
            color = m.group();
        }
        return variant + ":" + color;
    }

    private static Iterator<String> materialNameVariants(String name, String skin) {
        return new Iterator<String>() {
            int mask = (skin.charAt(0) == '-' ? 0b00 : 0b10) + (skin.endsWith("-") ? 0b0 : 0b1);
            int next = mask;

            @Override
            public boolean hasNext() {
                return next >= 0;
            }

            @Override
            public String next() {
                String out = applySkinToName(name, skin, next);
                next--;
                while (((next & mask) ^ next) != 0b00 && next >= 0) {
                    next--;
                }
                return out;
            }
        };
    }

    private static String applySkinToName(String name, String skin, int version) {
        Matcher m;
        if ((version & 0b10) == 0b10) {
            m = variantPattern.matcher(name);
            if (m.find()) {
                name = name.replace(m.group(), skin.substring(0, 2));
            }
        }
        if ((version & 0b1) == 0b1) {
            m = colorPattern.matcher(name);
            if (m.find()) {
                name = name.replace(m.group(), skin.substring(skin.indexOf(":") + 1));
            } else {
                name += skin.substring(skin.indexOf(":") + 1);
            }
        }
        return name;
    }

    private static class Skin implements Comparable<Skin> {

        final String variant;
        final String color;

        public static Skin fromMaterialName(String name) {
            String variant = null;
            String color = null;
            Matcher m = variantPattern.matcher(name);
            if (m.find()) {
                variant = m.group();
            }
            m = colorPattern.matcher(name);
            if (m.find()) {
                color = m.group();
            }
            if (variant == null && color == null) {
                return null;
            }
            return new Skin(variant, color);
        }

        public Skin(String variant, String color) {
            this.variant = variant;
            this.color = color;
        }

        @Override
        public int compareTo(Skin o) {
            int c1 = variant.compareTo(o.variant);
            if (c1 != 0) {
                return c1;
            } else {
                return color.compareTo(o.color);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (!Skin.class.isAssignableFrom(obj.getClass())) {
                return compareTo((Skin) obj) == 0;
            } else {
                return false;
            }
        }
    }
}
