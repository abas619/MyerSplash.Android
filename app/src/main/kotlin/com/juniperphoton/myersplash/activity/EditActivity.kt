package com.juniperphoton.myersplash.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.PipelineDraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.juniperphoton.flipperviewlib.FlipperView
import com.juniperphoton.myersplash.App
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.extension.getScreenHeight
import com.juniperphoton.myersplash.extension.hasNavigationBar
import com.juniperphoton.myersplash.utils.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream

class EditActivity : BaseActivity() {
    companion object {
        private const val TAG = "EditActivity"

        const val IMAGE_FILE_PATH = "image_file_path"
    }

    @BindView(R.id.edit_seek_bar_brightness)
    lateinit var brightnessSeekBar: SeekBar

    @BindView(R.id.edit_confirm_fab)
    lateinit var confirmFab: FloatingActionButton

    @BindView(R.id.edit_image_preview)
    lateinit var previewImageView: SimpleDraweeView

    @BindView(R.id.edit_mask)
    lateinit var maskView: View

    @BindView(R.id.edit_flipper_view)
    lateinit var flipperView: FlipperView

    @BindView(R.id.edit_progress_ring)
    lateinit var progressView: View

    @BindView(R.id.edit_home_preview)
    lateinit var homePreview: View

    @BindView(R.id.edit_progress_text)
    lateinit var progressText: TextView

    @BindView(R.id.edit_bottom_bar)
    lateinit var bottomBar: ViewGroup

    @BindView(R.id.edit_fabs_root)
    lateinit var fabsRoot: ViewGroup

    private var filePath: String? = null

    private var animating: Boolean = false

    private var showingPreview: Boolean = false
        set(value) {
            if (animating) {
                return
            }
            field = value
            toggleHomePreviewFadeAnimation(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        ButterKnife.bind(this)

        initView()
    }

    override fun onResume() {
        super.onResume()

        // Reset to the initial state anyway
        flipperView.next(0)
    }

    private fun initView() {
        filePath = intent.getStringExtra(IMAGE_FILE_PATH)
                ?: throw IllegalArgumentException("image url should not be null")

        if (!hasNavigationBar()) {
            val height = resources.getDimensionPixelSize(R.dimen.default_navigation_bar_height)
            bottomBar.setPadding(0, 0, 0, 0)

            val layoutParams = fabsRoot.layoutParams as FrameLayout.LayoutParams
            layoutParams.bottomMargin -= height
            fabsRoot.layoutParams = layoutParams
        }

        previewImageView.post {
            updatePreviewImage()
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SimpleOnSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                progressText.text = progress.toString()
                maskView.alpha = progress * 1f / 100
            }
        })

        val valueAnimator = ValueAnimator.ofFloat(0f, 360f)
        valueAnimator.addUpdateListener { animation -> progressView.rotation = animation.animatedValue as Float }
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.duration = 1200
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.start()
    }

    private fun updatePreviewImage() {
        val screenHeight = previewImageView.height

        Log.d(TAG, "scale height:$screenHeight")

        val request = ImageRequestBuilder.newBuilderWithSource(Uri.fromFile(File(filePath)))
                .setResizeOptions(ResizeOptions(screenHeight, screenHeight))
                .build()
        val controller = Fresco.newDraweeControllerBuilder()
                .setOldController(previewImageView.controller)
                .setImageRequest(request)
                .build() as PipelineDraweeController

        previewImageView.controller = controller
    }

    private fun toggleHomePreviewFadeAnimation(show: Boolean) {
        animating = true
        homePreview.animate().alpha(if (show) 1f else 0f)
                .setDuration(200)
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        animating = false
                    }
                })
                .start()
    }

    @OnClick(R.id.edit_confirm_fab)
    fun onClickConfirm() {
        if (maskView.alpha != 0f) {
            composeMask()
        } else if (filePath != null) {
            setAs(filePath!!)
        }
    }

    @OnClick(R.id.edit_preview_fab)
    fun onClickPreview() {
        showingPreview = !showingPreview
    }

    private fun setAs(path: String) {
        Log.d(TAG, "set as path:$path")
        val intent = IntentUtil.getSetAsWallpaperIntent(File(path))
        App.instance.startActivity(intent)
    }

    private fun composeMask() {
        flipperView.next()
        Observable.just(filePath)
                .subscribeOn(Schedulers.io())
                .map {
                    composeMaskInternal()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SimpleObserver<File>() {
                    override fun onNext(t: File) {
                        setAs(t.absolutePath)
                    }

                    override fun onError(e: Throwable) {
                        flipperView.next()
                        super.onError(e)
                        if (e is OutOfMemoryError) {
                            ToastService.sendShortToast("Out of memory occurs. Please contact the develop to solve this. :(")
                        }
                    }
                })
    }

    @WorkerThread
    private fun composeMaskInternal(): File {
        val bm = BitmapFactory.decodeFile(filePath)

        val ratio = bm.width * 1f / bm.height

        val targetHeight = this@EditActivity.getScreenHeight()
        val targetWidth = targetHeight * ratio

        Log.d(TAG, "target width:$targetWidth, targetHeight:$targetHeight")

        val scaleBm = Bitmap.createScaledBitmap(bm, targetWidth.toInt(), targetHeight, true)

        bm.recycle()

        Log.d(TAG, "scaled bitmap created")

        val w = scaleBm.width.toFloat()
        val h = scaleBm.height.toFloat()

        val finalBm = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(finalBm)
        val paint = Paint()
        paint.isDither = true
        c.drawBitmap(scaleBm, 0f, 0f, paint)

        val alpha = maskView.alpha
        paint.color = Color.argb((255 * alpha).toInt(), 0, 0, 0)
        paint.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, paint)

        Log.d(TAG, "final bitmap drawn")

        val finalFile = File(FileUtil.galleryPath, "final_dim_image.jpg")
        val fos = FileOutputStream(finalFile)
        fos.use {
            finalBm.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        scaleBm.recycle()
        finalBm.recycle()

        return finalFile
    }
}

open class SimpleOnSeekBarChangeListener : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }
}