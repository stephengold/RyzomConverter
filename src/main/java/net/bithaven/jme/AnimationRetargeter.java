package net.bithaven.jme;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

/**
 * Copyright Alweth on hub.jmonkeyengine.org forums 2016-2017
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
public final class AnimationRetargeter {

    private AnimationRetargeter() {
    }

    //TODO: If a mesh has already been cloned and retargeted to the newSkeleton, then just pass that back instead of deepCloning it again and
    //everything.
    public static Spatial retarget(Spatial spatial, Skeleton newSkeleton) {
        retarget(spatial, null, newSkeleton);
        //spatial.updateGeometricState();
        return spatial;
    }

    public static void retarget(Spatial spatial, Skeleton oldSkeleton, Skeleton newSkeleton) {
        Skeleton use = oldSkeleton;
        SkeletonControl sc = spatial.getControl(SkeletonControl.class);
        if (sc != null) {
            use = sc.getSkeleton();
            spatial.removeControl(SkeletonControl.class);
            //sc = new SkeletonControl(newSkeleton);
            //spatial.addControl(sc);
        }
        spatial.removeControl(AnimControl.class);

        if (use != null && spatial instanceof Geometry) {
            Mesh mesh = ((Geometry) spatial).getMesh();
            if (mesh != null) {
                mesh = mesh.deepClone();
                retarget(mesh, use, newSkeleton);
                ((Geometry) spatial).setMesh(mesh);
                //((Geometry)spatial).getMaterial().clearParam("BoneMatrices");
                Material m = ((Geometry) spatial).getMaterial();
                if (m != null) {
                    m.clearParam("BoneMatrices");
                }
            }
        } else if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                retarget(child, use, newSkeleton);
            }
        }
    }

    public static void retarget(Mesh mesh, Skeleton oldSkeleton, Skeleton newSkeleton) {
        byte[] map = generateMap(oldSkeleton, newSkeleton);
        int count = map.length;
        retargetBuffer(mesh, map, mesh.getBuffer(Type.BoneIndex), count);
        retargetBuffer(mesh, map, mesh.getBuffer(Type.HWBoneIndex), count);
    }

    private static void retargetBuffer(Mesh mesh, byte[] map, VertexBuffer buff, int count) {
        if (buff != null) {
            int components = buff.getNumComponents();
            int elements = buff.getNumElements();

            for (int i = 0; i < elements; i++) {
                for (int j = 0; j < components; j++) {
                    byte o = (byte) buff.getElementComponent(i, j);

                    if (o >= 0 && o < count) {
                        byte n = map[o];
                        if (n != -1) {
                            buff.setElementComponent(i, j, n);
                        }
                    }
                }
            }

            mesh.prepareForAnim(true); //TODO: Check if this is actually needed.
        }
    }

    public static void retarget(Animation a, Skeleton oldSkeleton, Skeleton newSkeleton) {
        byte[] map = generateMap(oldSkeleton, newSkeleton);

        Track[] tracks = a.getTracks();
        Track[] to = new Track[tracks.length];

        int toI = 0;
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i] instanceof BoneTrack) {
                BoneTrack t = (BoneTrack) tracks[i];
                int index = map[t.getTargetBoneIndex()];
                if (index != -1) {
                    Vector3f[] scales = t.getScales();
                    if (scales != null) {
                        to[toI] = new BoneTrack(index, t.getTimes(), t.getTranslations(), t.getRotations(), scales);
                    } else {
                        to[toI] = new BoneTrack(index, t.getTimes(), t.getTranslations(), t.getRotations());
                    }
                    a.removeTrack(t);
                    toI++;
                }
            }
        }
        Track[] out = new Track[toI];
        System.arraycopy(to, 0, out, 0, toI);
        a.setTracks(out);
    }

    private static byte[] generateMap(Skeleton from, Skeleton to) {
        int count = from.getBoneCount();
        byte[] map = new byte[count];

        for (int i = 0; i < count; i++) {
            Bone bone = from.getBone(i);
            int index = to.getBoneIndex(bone.getName());
            while (index == -1) {
                bone = bone.getParent();
                if (bone == null) {
                    break;
                }
                index = to.getBoneIndex(bone.getName());
            }
            map[i] = (byte) index;
        }

        return map;
    }
}
