import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import com.example.firsttrykhs.R

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onVideoEnded: () -> Unit
) {
    AndroidView(
        factory = { context ->
            val videoView = VideoView(context)

            val videoPath = "android.resource://${context.packageName}/${R.raw.splashscreen}"
            Log.d("SplashScreen", "Video path: $videoPath")  // <-- Add this line here

            videoView.setVideoURI(Uri.parse(videoPath))

            videoView.setOnPreparedListener {
                Log.d("SplashScreen", "Video prepared, starting playback")  // Optional: extra debug
                videoView.start()
            }

            videoView.setOnCompletionListener {
                Log.d("SplashScreen", "Video completed")
                onVideoEnded()
            }

            videoView
        },
        modifier = Modifier.fillMaxSize()
    )

}
