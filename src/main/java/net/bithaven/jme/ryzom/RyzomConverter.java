package net.bithaven.jme.ryzom;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.logging.Level.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import com.jme3.texture.Texture;

import net.bithaven.jme.IQELoader;
import net.bithaven.jme.ryzom.Ryzom.ModelPart;

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
public final class RyzomConverter extends SimpleApplication {

    private static final Logger logger = Logger.getLogger(RyzomConverter.class.getName());

    private static AssetManager am;
    private static String ryzomRoot;
    private static Path ryzomRootPath;
    private static String actorsDir = "ryzom-assets/actors";
    private static String actorsRoot;
    private static boolean noAnimations = false;
    private static boolean noModels = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: The path to the directory containing the Ryzom directory \"ryzom-assets\" must be passed as the first argument.");
            return;
        } else {
            ryzomRoot = args[0];
            ryzomRootPath = FileSystems.getDefault().getPath(ryzomRoot);
            actorsRoot = ryzomRoot + "/" + actorsDir;
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("noanimations")) {
                    noAnimations = true;
                }
                if (args[i].equals("nomodels")) {
                    noModels = true;
                }
            }
            RyzomConverter app = new RyzomConverter();
            app.start(JmeContext.Type.Headless);
        }
    }

    private RyzomConverter() {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false));
    }

    /**
     * Initializes the client app. Inherited from JMonkey; called by JMonkey;
     * don't call.
     */
    @Override
    public void simpleInitApp() {

        // init stuff that is independent of whether state is PAUSED or RUNNING
        am = getAssetManager();
        am.registerLocator(ryzomRoot, FileLocator.class);
        am.registerLoader(IQELoader.class, "iqe");

        am.registerLocator("assets", FileLocator.class);
        am.unregisterLocator("/", ClasspathLocator.class);
        am.registerLocator("/", ClasspathLocator.class);

    }

    /**
     * Inherited from JMonkey; called by JMonkey; don't call.
     *
     * @param tpf The number of seconds that has past since this was last
     * called. Usually much less than {@code 1f}.
     *
     */
    @Override
    public void simpleUpdate(float tpf) {
        if (!noModels) {
            for (ModelPart part : ModelPart.values()) {
                DirectoryStream<Path> ds = null;
                try {
                    ds = Files.newDirectoryStream(
                            FileSystems.getDefault().getPath(actorsRoot + "/" + part.dir),
                            part.fileFilter);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                for (Path p : ds) {
                    logger.log(INFO, "Loading " + part.toString() + ": " + p.getFileName().toString());
                    List<Spatial> variants = Ryzom.generateSpatialsFromPath(am, p, ryzomRootPath);
                    logger.log(INFO, "Exporting " + variants.size() + " variants.");
                    for (Spatial s : variants) {
                        s.setName(s.getName() + "@" + s.getUserData("ryzom_skin"));
                        exportSpatial(s);
                    }
                }
                try {
                    ds.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (!noAnimations) {
            HashMap<String, Node> nodes = new HashMap<String, Node>();
            HashMap<String, AnimControl> animControls = new HashMap<String, AnimControl>();
            animControls.put("ca_hof", null);
            animControls.put("ca_hom", null);
            animControls.put("ge_hof", null);
            animControls.put("ge_hom", null);
            for (Map.Entry<String, AnimControl> entry : animControls.entrySet()) {
                String code = entry.getKey();
                Skeleton sk = Ryzom.getSkeleton(am, code);
                AnimControl aControl = new AnimControl(sk);
                entry.setValue(aControl);
                Node node = new Node("animations_" + code);
                node.addControl(aControl);
                SkeletonControl sc = new SkeletonControl(sk);
                //sc.setHardwareSkinningPreferred(true);
                node.addControl(sc);
                nodes.put(code, node);
            }

            DirectoryStream<Path> animationsIn = null;
            try {
                animationsIn = Files.newDirectoryStream(
                        FileSystems.getDefault().getPath(actorsRoot + "/anims"), "*.iqe");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            for (Path p : animationsIn) {
                String fileName = p.getFileName().toString();
                logger.log(INFO, "Loading animation: " + fileName);
                String name = fileName.substring(0, fileName.length() - 4);
                String code = Ryzom.extractSkeletonCode(fileName);
                AnimControl ac = animControls.get(code);
                Ryzom.addAnim(am, ac, name, code);
                //code = Ryzom.switchSkeletonCodeRace(code);
                //ac = animControls.get(code);
                //Ryzom.addAnim(am, ac, name, code);
            }

            Object[] keys = nodes.keySet().toArray();
            for (Object k : keys) {
                logger.log(INFO, "Exporting animations: " + nodes.get(k).getName());
                exportSpatial(nodes.get(k));
                nodes.remove(k);
            }
        }

        stop();
    }

    public void exportSpatial(Spatial s) {
        String code = s.getUserData("ryzom_skeleton");
        if (code != null && !code.startsWith("ca")) {
            Spatial o = s.getUserData("ryzom_alternate");
            if (o != null) {
                s = o;
            }
        }
        s = s.deepClone();
        Path p = FileSystems.getDefault().getPath("assets", "ryzom-assets", "export");
        Path tex = p.resolve("textures");
        try {
            if (!Files.exists(p)) {
                Files.createDirectory(p);
            }
            if (!Files.exists(tex)) {
                Files.createDirectory(tex);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        s.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Material m = ((Geometry) spatial).getMaterial();
                    if (m != null) {
                        MatParamTexture param = m.getTextureParam("ColorMap");
                        if (param != null) {
                            Texture t = param.getTextureValue();
                            if (t != null) {
                                TextureKey key = (TextureKey) t.getKey();
                                File from = new File(ryzomRoot + "/" + key.getName());
                                Path toPath = tex.resolve(from.getName());
                                File to = toPath.toFile();
                                try {
                                    FileUtils.copyFile(from, to);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                key = new TextureKey(toPath.subpath(1, toPath.getNameCount()).toString(), key.isFlipY());
                                t = am.loadTexture(key);
                                m.setTexture("ColorMap", t);
                            }
                        }
                    }
                }
            }
        });
        File out = new File(p.toString(), s.getName() + ".j3o");
        try {
            BinaryExporter.getInstance().save(s, out);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
