package eu.tutorials.lab_week_11_b

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 3
    }

    private lateinit var providerFileManager: ProviderFileManager
    private var photoInfo: FileInfo? = null
    private var videoInfo: FileInfo? = null
    private var isCapturingVideo = false
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        providerFileManager =
            ProviderFileManager(
                applicationContext,
                FileHelper(applicationContext),
                contentResolver,
                Executors.newSingleThreadExecutor(),
                MediaContentHelper()
            )


        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) {
                providerFileManager.insertImageToStore(photoInfo)
            }
        takeVideoLauncher =
            registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
                providerFileManager.insertVideoToStore(videoInfo)
            }

        findViewById<Button>(R.id.photo_button).setOnClickListener {
            isCapturingVideo = false
            checkStoragePermission {
                openImageCapture()
            }
        }
        findViewById<Button>(R.id.video_button).setOnClickListener {
            isCapturingVideo = true
            checkStoragePermission {
                openVideoCapture()
            }
        }
    }

    private fun openImageCapture() {
        photoInfo =
            providerFileManager.generatePhotoUri(System.currentTimeMillis())
        photoInfo?.uri?.let { takePictureLauncher.launch(it) }
    }

    // Open the camera to capture a video
    private fun openVideoCapture() {
        videoInfo =
            providerFileManager.generateVideoUri(System.currentTimeMillis())
        videoInfo?.uri?.let { takeVideoLauncher.launch(it) }
    }

    // Check the storage permission
    // For Android 10 and above, the permission is not required
    // For Android 9 and below, the permission is required
    private fun checkStoragePermission(onPermissionGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT <
            android.os.Build.VERSION_CODES.Q) {
            //Check for the WRITE_EXTERNAL_STORAGE permission
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                // If the permission is granted
                PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGranted()
                }
                // if the permission is not granted, request the permission
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            }
        } else {
            onPermissionGranted()
        }
    }

    // For android 9 and below
    // Handle the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions,
            grantResults)
        when (requestCode) {
                    REQUEST_EXTERNAL_STORAGE -> {
                // If granted, open the camera
                if ((grantResults.isNotEmpty() && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED)) {
                    if (isCapturingVideo) {
                        openVideoCapture()
                    } else {
                        openImageCapture()
                    }
                }
                return
            }
        }
    }
}
