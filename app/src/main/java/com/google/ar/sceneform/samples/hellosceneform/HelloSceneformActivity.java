/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.CollisionShape;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseGesture;
import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.BaseTransformationController;
import com.google.ar.sceneform.ux.PinchGesture;
import com.google.ar.sceneform.ux.PinchGestureRecognizer;
import com.google.ar.sceneform.ux.ScaleController;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity implements BaseGesture.OnGestureEventListener<PinchGesture>, Node.OnTapListener {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    ArrayList<Float> arrayList1 = new ArrayList<Float>();
    ArrayList<Float> arrayList2 = new ArrayList<Float>();

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private AnchorNode lastAnchorNode;
    private TextView txtDistance;
    Button btnDist, btnHeight;
    ModelRenderable cubeRenderable;
    static int tap = 0;
    private AnchorNode anchorNode;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        btnDist = (Button) findViewById(R.id.btnDistance);
        btnHeight = (Button) findViewById(R.id.btnHeight);


        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.GREEN))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.01f, 0.1f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }

                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    Pose pose = anchor.getPose();

                    if (arrayList1.isEmpty()) {
                        arrayList1.add(pose.tx());
                        arrayList1.add(pose.ty());
                        arrayList1.add(pose.tz());
                    } else {
                        arrayList2.add(pose.tx());
                        arrayList2.add(pose.ty());
                        arrayList2.add(pose.tz());
                    }

                    if (!arrayList1.isEmpty() && !arrayList2.isEmpty()) {
                        double d = getDistanceMeters(arrayList1, arrayList2);
                        Log.e("Dis: ", String.valueOf(d));
                        txtDistance.setText(String.valueOf(d));
                    }


                    TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                    transformableNode.setParent(anchorNode);
                    transformableNode.setRenderable(cubeRenderable);
                    transformableNode.select();
                    ScaleController scaleController = transformableNode.getScaleController();
                    scaleController.setMaxScale(10f);
                    scaleController.setMinScale(0.01f);
                    if (transformableNode.isSelected()){
                        Box box = (Box) transformableNode.getRenderable().getCollisionShape();
                        Vector3 renderableSize = box.getSize();
                        Vector3 transformableNodeScale = transformableNode.getWorldScale();
                        Vector3 finalSize =
                                new Vector3(
                                        renderableSize.x * transformableNodeScale.x,
                                        renderableSize.y * transformableNodeScale.y,
                                        renderableSize.z * transformableNodeScale.z);
                        Log.e("FinalSize: ", String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z));
                        Toast.makeText(this, "Final Size is " + String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z) , Toast.LENGTH_SHORT).show();
                    }


                    transformableNode.setOnTapListener(this);


                    tap++;
                    if (tap > 2) {
                        onClear();
                        arrayList1.clear();
                        arrayList2.clear();
                        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
                        tap = 0;
                    }
                });

    }

    private void onClear() {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
    }

    private void setNewAnchor(Anchor newAnchor) {
        AnchorNode newAnchorNode = null;

        if (anchorNode != null && newAnchor != null) {
            // Create a new anchor node and move the children over.
            newAnchorNode = new AnchorNode(newAnchor);
            newAnchorNode.setParent(arFragment.getArSceneView().getScene());
            List<Node> children = new ArrayList<>(anchorNode.getChildren());
            for (Node child : children) {
                child.setParent(newAnchorNode);
            }
        } else if (anchorNode == null && newAnchor != null) {
            // First anchor node created, add Andy as a child.
            newAnchorNode = new AnchorNode(newAnchor);
            newAnchorNode.setParent(arFragment.getArSceneView().getScene());

            Node andy = new Node();
            andy.setRenderable(cubeRenderable);
            andy.setParent(newAnchorNode);
        } else {
            // Just clean up the anchor node.
            if (anchorNode != null && anchorNode.getAnchor() != null) {
                anchorNode.getAnchor().detach();
                anchorNode.setParent(null);
                anchorNode = null;
            }
        }
        anchorNode = newAnchorNode;
    }

    private void addLineBetweenHits(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

        int val = motionEvent.getActionMasked();
        float axisVal = motionEvent.getAxisValue(MotionEvent.AXIS_X, motionEvent.getPointerId(motionEvent.getPointerCount() - 1));
        Log.e("Values:", String.valueOf(val) + String.valueOf(axisVal));
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);


        if (lastAnchorNode != null) {
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            Vector3 point1, point2;
            point1 = lastAnchorNode.getWorldPosition();
            point2 = anchorNode.getWorldPosition();

        /*
            First, find the vector extending between the two points and define a look rotation
            in terms of this Vector.
        */
            final Vector3 difference = Vector3.subtract(point1, point2);
            final Vector3 directionFromTopToBottom = difference.normalized();
            final Quaternion rotationFromAToB =
                    Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
            MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 244))
                    .thenAccept(
                            material -> {
                                /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                       to extend to the necessary length.  */
                                ModelRenderable model = ShapeFactory.makeCube(
                                        new Vector3(.01f, .01f, difference.length()),
                                        Vector3.zero(), material);
                                /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                       the midpoint between the given points . */
                                Node node = new Node();
                                node.setParent(anchorNode);
                                node.setRenderable(model);
                                node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                                node.setWorldRotation(rotationFromAToB);
                            }
                    );
            lastAnchorNode = anchorNode;
        }
    }

    private double getDistanceMeters(ArrayList<Float> arayList1, ArrayList<Float> arrayList2) {

        float distanceX = arayList1.get(0) - arrayList2.get(0);
        float distanceY = arayList1.get(1) - arrayList2.get(1);
        float distanceZ = arayList1.get(2) - arrayList2.get(2);

        return Math.sqrt(distanceX * distanceX +
                distanceY * distanceY +
                distanceZ * distanceZ);
    }

    float getMetersBetweenAnchors(Anchor anchor1, Anchor anchor2) {
        float[] distance_vector = anchor1.getPose().inverse()
                .compose(anchor2.getPose()).getTranslation();
        float totalDistanceSquared = 0;
        for (int i = 0; i < 3; ++i)
            totalDistanceSquared += distance_vector[i] * distance_vector[i];
        return (float) Math.sqrt(totalDistanceSquared);
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onUpdated(PinchGesture gesture) {
        Vector3 vector = gesture.getTargetNode().getWorldPosition();
        Log.i("Vector:", String.valueOf(vector.x + "Y: " + vector.y + "Z: " + vector.z));
    }

    @Override
    public void onFinished(PinchGesture gesture) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();
        Box box = (Box) node.getRenderable().getCollisionShape();
        Vector3 renderableSize = box.getSize();
        Vector3 transformableNodeScale = node.getWorldScale();
        Vector3 finalSize =
                new Vector3(
                        renderableSize.x * transformableNodeScale.x,
                        renderableSize.y * transformableNodeScale.y,
                        renderableSize.z * transformableNodeScale.z);
        Log.e("FinalSize: ", String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z));
        Toast.makeText(this, "Final Size is " + String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z) , Toast.LENGTH_SHORT).show();
        //CollisionShape shape = node.getCollisionShape();
        Matrix matrix = node.getWorldModelMatrix();
        Vector3 newVector = matrix.extractScale();
        Vector3 vector31 = node.getWorldScale().normalized();
        Vector3 vec = node.getLocalPosition();
        Vector3 vector = node.getWorldPosition();
        Vector3 vector32 = node.getLocalScale();
        vec.normalized();
        Log.e("Vector Local Position: ", String.valueOf(vec.x + " " + vec.y + " " + vec.z));
        Log.e("Vector World Position: ", String.valueOf(vector.x + " " + vector.y + " " + vector.z));
        Log.e("Vector World Scale: ", String.valueOf(vector31.x + " " + vector31.y + " " + vector31.z));
        Log.e("Main Local Scale: ", String.valueOf(vector32.x + " " + vector32.y + " " + vector32.z));
        Log.e("Vector New Size: ", String.valueOf(vec.x + " " + vec.y + " " + vec.z + " " + vec.normalized() + " " + vec.length()));
        Log.e("Matrix world scale: ", String.valueOf(newVector.x + " " + newVector.y + " " + newVector.z));
    }
}
