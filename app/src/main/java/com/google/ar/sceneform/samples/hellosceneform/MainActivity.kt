package com.google.ar.sceneform.samples.hellosceneform

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import android.widget.Toast.makeText
import com.google.ar.core.Anchor
import com.google.ar.sceneform.*
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_ux.*
import java.util.*


@SuppressLint("Registered")
class MainActivity : AppCompatActivity(), Node.OnTapListener, Scene.OnUpdateListener {

    val TAG = MainActivity::class.simpleName
    val MIN_OPENGL_VERSION: Double = 3.0
    var arrayList1 = ArrayList<Float>()
    var arrayList2 = ArrayList<Float>()
    lateinit var arFragment: ArFragment
    var lastAnchorNode: AnchorNode? = null
    lateinit var cubeRenderable: ModelRenderable
    lateinit var heightRenderable: ModelRenderable
    var btnHeightClicked: Boolean = false
    var btnLengthClicked: Boolean = false
    var point1: Vector3? = null
    var point2: Vector3? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ux)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        btnDistance.setOnClickListener {
            btnLengthClicked = true
            btnHeightClicked = false
            onClear()
        }

        btnHeight.setOnClickListener {
            btnLengthClicked = false
            btnHeightClicked = true
            onClear()
        }

        clear.setOnClickListener {
            onClear()
        }

        MaterialFactory.makeTransparentWithColor(this, Color(0F, 0F, 244F))
                .thenAccept { material ->
                    val vector3 = Vector3(0.01f, 0.01f, 0.01f)
                    cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material)
                    cubeRenderable.isShadowReceiver = false
                    cubeRenderable.isShadowCaster = false
                }

        MaterialFactory.makeTransparentWithColor(this, Color(0F, 0F, 244F))
                .thenAccept { material ->
                    val vector3 = Vector3(0.007f, 0.1f, 0.007f)
                    heightRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material)
                    heightRenderable.isShadowCaster = false
                    heightRenderable.isShadowReceiver = false
                }

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (cubeRenderable == null) {
                return@setOnTapArPlaneListener
            }

            if (btnHeightClicked) {
                if (lastAnchorNode != null) {
                    makeText(this, "Please click ${clear.text} button", Toast.LENGTH_SHORT).show()
                    return@setOnTapArPlaneListener
                }
                val anchor: Anchor = hitResult.createAnchor()
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arFragment.arSceneView.scene)
                val transformableNode = TransformableNode(arFragment.transformationSystem)
                transformableNode.setParent(anchorNode)
                transformableNode.renderable = heightRenderable
                transformableNode.select()
                val scaleController = transformableNode.scaleController
                scaleController.maxScale = 10f
                scaleController.minScale = 0.01f
                transformableNode.setOnTapListener(this)
                arFragment.arSceneView.scene.addOnUpdateListener(this)
                lastAnchorNode = anchorNode
            }

            if (btnLengthClicked) {
                if (lastAnchorNode == null) {
                    val anchor = hitResult.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)

                    val pose = anchor.pose
                    if (arrayList1.isEmpty()) {
                        arrayList1.add(pose.tx())
                        arrayList1.add(pose.ty())
                        arrayList1.add(pose.tz())
                    }
                    val transformableNode = TransformableNode(arFragment.transformationSystem)
                    transformableNode.setParent(anchorNode)
                    transformableNode.renderable = cubeRenderable
                    transformableNode.select()
                    lastAnchorNode = anchorNode
                } else {
                    val value = motionEvent.actionMasked
                    val axisVal = motionEvent.getAxisValue(MotionEvent.AXIS_X, motionEvent.getPointerId(motionEvent.pointerCount - 1))
                    Log.e("Values:", value.toString() + axisVal.toString())
                    val anchor = hitResult.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)

                    val pose = anchor.pose
                    if (arrayList2.isEmpty()) {
                        arrayList2.add(pose.tx())
                        arrayList2.add(pose.ty())
                        arrayList2.add(pose.tz())
                        val d = getDistanceMeters(arrayList1, arrayList2)
                        txtDistance.text = "Distance: $d"
                    } else {
                        arrayList1.clear()
                        arrayList1.addAll(arrayList2)
                        arrayList2.clear()
                        arrayList2.add(pose.tx())
                        arrayList2.add(pose.ty())
                        arrayList2.add(pose.tz())
                        val d = getDistanceMeters(arrayList1, arrayList2)
                        txtDistance.text = "Distance: $d"
                    }

                    val transformableNode = TransformableNode(arFragment.transformationSystem)
                    transformableNode.setParent(anchorNode)
                    transformableNode.renderable = cubeRenderable
                    transformableNode.select()

                    val point1: Vector3 = lastAnchorNode!!.worldPosition
                    val point2: Vector3 = anchorNode.worldPosition

                    val difference = Vector3.subtract(point1, point2)
                    val directionFromTopToBottom = difference.normalized()
                    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
                    MaterialFactory.makeOpaqueWithColor(applicationContext, Color(0F, 255F, 244F))
                            .thenAccept { material ->
                                val modelRenderable = ShapeFactory.makeCube(
                                        Vector3(.01f, .01f, difference.length()),
                                        Vector3.zero(), material)
                                val node = Node()
                                node.setParent(anchorNode)
                                node.renderable = modelRenderable
                                node.worldPosition = Vector3.add(point1, point2).scaled(.5f)
                                node.worldRotation = rotationFromAToB
                            }
                    lastAnchorNode = anchorNode
                }
            }

        }


    }

    private fun getDistanceMeters(arrayList1: ArrayList<Float>, arrayList2: ArrayList<Float>): Any {
        val distanceX = arrayList1[0] - arrayList2[0]
        val distanceY = arrayList1[1] - arrayList2[1]
        val distanceZ = arrayList1[2] - arrayList2[2]
        return Math.sqrt((distanceX * distanceX +
                distanceY * distanceY +
                distanceZ * distanceZ).toDouble())
    }

    private fun onClear() {
        val children = ArrayList(arFragment.arSceneView.scene.children)
        for (node in children) {
            if (node is AnchorNode) {
                if (node.anchor != null) {
                    node.anchor.detach()
                }
            }
            if (node !is Camera && node !is Sun) {
                node.setParent(null)
            }
        }
        arrayList1.clear()
        arrayList2.clear()
        lastAnchorNode = null
        point1 = null
        point2 = null
        txtDistance.text = null
    }

    @SuppressLint("SetTextI18n")
    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
        val node = p0?.node
        val box = node?.renderable?.collisionShape as Box
        val renderableSize = box.size
        val transformableNodeScale = node.worldScale
        val finalSize = Vector3(
                renderableSize.x * transformableNodeScale.x,
                renderableSize.y * transformableNodeScale.y,
                renderableSize.z * transformableNodeScale.z)
        txtDistance.text = "Height: ${finalSize.y}"
        Log.e("FinalSize:", "${finalSize.x} ${finalSize.y} ${finalSize.z}")
    }

    override fun onUpdate(p0: FrameTime?) {

    }

    @SuppressLint("ObsoleteSdkInt")
    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            makeText(applicationContext, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        return true
    }


}