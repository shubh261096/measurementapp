package com.google.ar.sceneform.samples.hellosceneform;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.ScaleController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class HelloSceneformActivity extends AppCompatActivity implements Node.OnTapListener, Scene.OnUpdateListener {
    private static final String TAG = HelloSceneformActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    ArrayList<Float> arrayList1 = new ArrayList<>();
    ArrayList<Float> arrayList2 = new ArrayList<>();
    private ArFragment arFragment;
    private AnchorNode lastAnchorNode;
    private TextView txtDistance;
    Button btnDist, btnHeight, btnClear;
    ModelRenderable cubeRenderable, heightRenderable;
    boolean btnHeightClicked, btnLengthClicked;
    Vector3 point1, point2;

    @SuppressLint("SetTextI18n")
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        txtDistance = findViewById(R.id.txtDistance);
        btnDist = findViewById(R.id.btnDistance);
        btnDist.setOnClickListener(v -> {
            btnLengthClicked = true;
            btnHeightClicked = false;
            onClear();
        });
        btnHeight = findViewById(R.id.btnHeight);
        btnHeight.setOnClickListener(v -> {
            btnHeightClicked = true;
            btnLengthClicked = false;
            onClear();
        });
        btnClear = findViewById(R.id.clear);
        btnClear.setOnClickListener(v -> onClear());

        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.01f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });

        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.007f, 0.1f, 0.007f);
                            heightRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            heightRenderable.setShadowCaster(false);
                            heightRenderable.setShadowReceiver(false);
                        });


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (cubeRenderable == null) {
                        return;
                    }

                    if (btnHeightClicked) {
                        if (lastAnchorNode != null) {
                            Toast.makeText(this, "Please click clear button", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Anchor anchor = hitResult.createAnchor();
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setParent(anchorNode);
                        transformableNode.setRenderable(heightRenderable);
                        transformableNode.select();
                        ScaleController scaleController = transformableNode.getScaleController();
                        scaleController.setMaxScale(10f);
                        scaleController.setMinScale(0.01f);
                        transformableNode.setOnTapListener(this);
                        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
                        lastAnchorNode = anchorNode;
                    }

                    if (btnLengthClicked) {
                        if (lastAnchorNode == null) {
                            Anchor anchor = hitResult.createAnchor();
                            AnchorNode anchorNode = new AnchorNode(anchor);
                            anchorNode.setParent(arFragment.getArSceneView().getScene());

                            Pose pose = anchor.getPose();
                            if (arrayList1.isEmpty()) {
                                arrayList1.add(pose.tx());
                                arrayList1.add(pose.ty());
                                arrayList1.add(pose.tz());
                            }
                            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                            transformableNode.setParent(anchorNode);
                            transformableNode.setRenderable(cubeRenderable);
                            transformableNode.select();
                            lastAnchorNode = anchorNode;
                        } else {
                            int val = motionEvent.getActionMasked();
                            float axisVal = motionEvent.getAxisValue(MotionEvent.AXIS_X, motionEvent.getPointerId(motionEvent.getPointerCount() - 1));
                            Log.e("Values:", String.valueOf(val) + String.valueOf(axisVal));
                            Anchor anchor = hitResult.createAnchor();
                            AnchorNode anchorNode = new AnchorNode(anchor);
                            anchorNode.setParent(arFragment.getArSceneView().getScene());

                            Pose pose = anchor.getPose();


                            if (arrayList2.isEmpty()) {
                                arrayList2.add(pose.tx());
                                arrayList2.add(pose.ty());
                                arrayList2.add(pose.tz());
                                float d = getDistanceMeters(arrayList1, arrayList2);
                                txtDistance.setText("Distance: " + String.valueOf(d));
                            } else {
                                arrayList1.clear();
                                arrayList1.addAll(arrayList2);
                                arrayList2.clear();
                                arrayList2.add(pose.tx());
                                arrayList2.add(pose.ty());
                                arrayList2.add(pose.tz());
                                float d = getDistanceMeters(arrayList1, arrayList2);
                                txtDistance.setText("Distance: " + String.valueOf(d));
                            }

                            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                            transformableNode.setParent(anchorNode);
                            transformableNode.setRenderable(cubeRenderable);
                            transformableNode.select();

                            Vector3 point1, point2;
                            point1 = lastAnchorNode.getWorldPosition();
                            point2 = anchorNode.getWorldPosition();

                            final Vector3 difference = Vector3.subtract(point1, point2);
                            final Vector3 directionFromTopToBottom = difference.normalized();
                            final Quaternion rotationFromAToB =
                                    Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
                            MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 244))
                                    .thenAccept(
                                            material -> {
                                                ModelRenderable model = ShapeFactory.makeCube(
                                                        new Vector3(.01f, .01f, difference.length()),
                                                        Vector3.zero(), material);
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
        arrayList1.clear();
        arrayList2.clear();
        lastAnchorNode = null;
        point1 = null;
        point2 = null;
        txtDistance.setText("");
    }

    private float getDistanceMeters(ArrayList<Float> arayList1, ArrayList<Float> arrayList2) {

        float distanceX = arayList1.get(0) - arrayList2.get(0);
        float distanceY = arayList1.get(1) - arrayList2.get(1);
        float distanceZ = arayList1.get(2) - arrayList2.get(2);
        return (float) Math.sqrt(distanceX * distanceX +
                distanceY * distanceY +
                distanceZ * distanceZ);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Node node = hitTestResult.getNode();
        Box box = (Box) node.getRenderable().getCollisionShape();
        assert box != null;
        Vector3 renderableSize = box.getSize();
        Vector3 transformableNodeScale = node.getWorldScale();
        Vector3 finalSize =
                new Vector3(
                        renderableSize.x * transformableNodeScale.x,
                        renderableSize.y * transformableNodeScale.y,
                        renderableSize.z * transformableNodeScale.z);
        txtDistance.setText("Height: " + String.valueOf(finalSize.y));
        Log.e("FinalSize: ", String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z));
        //Toast.makeText(this, "Final Size is " + String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
//        Collection<Anchor> updatedAnchors = frame.getUpdatedAnchors();
//        for (Anchor anchor : updatedAnchors) {
//            Handle updated anchors...
//        }
    }
}
