package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import android.content.pm.ActivityInfo
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme

// Theme Colors inspired by Natural Tones
val ThemeBg = Color(0xFFFDF7FF)
val ThemeTextPrimary = Color(0xFF1D1B20)
val ThemeTextSecondary = Color(0xFF49454F)
val ThemePrimary = Color(0xFF6750A4)
val ThemePrimaryLight = Color(0xFFEADDFF)
val ThemeBorder = Color(0xFFCAC4D0)
val ThemeBorderLight = Color(0xFFD0BCFF)
val ThemeBottomBg = Color(0xFFF3EDF7)

// Video Data Model
data class VideoItem(
    val id: String,
    val title: String,
    val videoUrl: String, // Can be web URL or content URI string
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = false,
    val duration: String = "0:30"
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    
    // Splash Screen State
    var showSplashScreen by remember { mutableStateOf(true) }
    
    // Permission States
    var hasContactsPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    
    // Main UI transition state based on permissions
    var showPermissionPage by remember { mutableStateOf(true) }
    var isInitialCheck by remember { mutableStateOf(true) }

    // Splash screen timer
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSplashScreen = false
    }

    // Helper to check current permissions
    fun updatePermissionStates() {
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val allGranted = hasContactsPermission && hasNotificationPermission && hasStoragePermission

    LaunchedEffect(allGranted) {
        if (allGranted) {
            if (isInitialCheck) {
                showPermissionPage = false
                isInitialCheck = false
            } else {
                // Wait for 1.2s so user can visually see red 'Required' transition to green 'Success'
                kotlinx.coroutines.delay(1200)
                showPermissionPage = false
            }
        } else {
            showPermissionPage = true
            isInitialCheck = false
        }
    }

    // Check states initially and when app resumes
    LaunchedEffect(Unit) {
        updatePermissionStates()
    }

    // Handle lifecycle or returning to app to check if Settings permission was granted
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updatePermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSplashScreen) {
        SplashScreen()
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ThemeBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPermissionPage) {
                    PermissionScreen(
                        hasContacts = hasContactsPermission,
                        hasNotification = hasNotificationPermission,
                        hasStorage = hasStoragePermission,
                        onCheckPermissions = { updatePermissionStates() }
                    )
                } else {
                    VideoGridHomeScreen()
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D1233),
                        Color(0xFF0F081D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant container for the logo
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        color = Color(0xFF281A45),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .border(
                        BorderStroke(2.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.img_app_logo_1782630778041,
                    contentDescription = "CP HUB Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "CP HUB",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Premium Media Hub",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFFCAC4D0),
                trackColor = Color(0xFF1D1233),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PermissionScreen(
    hasContacts: Boolean,
    hasNotification: Boolean,
    hasStorage: Boolean,
    onCheckPermissions: () -> Unit
) {
    val context = LocalContext.current

    // Standard Permissions Launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        onCheckPermissions()
        
        // Notify user if some are still missing
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: hasContacts
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotification
        } else {
            true
        }
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: hasStorage
        } else {
            Environment.isExternalStorageManager()
        }

        if (!contactsGranted || !notificationGranted || !storageGranted) {
            Toast.makeText(
                context, 
                "Machan, apita anivaryenma permission thuna ona video play karanna!", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Title Section with Natural Tones Theme Style
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(ThemePrimary, ThemeBorderLight)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Shield Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = ThemeTextPrimary,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Machan, app eka wada karanna permission thuna anivaren allow karanna ona.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ThemeTextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // List of Permissions Display Cards inside Light Highlight container
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 24.dp)
        ) {
            PermissionItemCard(
                title = "All Files Permission",
                desc = "Videos check karanna thava add karanna me permission eka anivaren ona.",
                isGranted = hasStorage,
                icon = Icons.Default.Info,
                accentColor = Color(0xFF6750A4)
            )

            PermissionItemCard(
                title = "Notifications Permission",
                desc = "Upload wenakota alert danna notifications support eka ona venava.",
                isGranted = hasNotification,
                icon = Icons.Default.Notifications,
                accentColor = Color(0xFF6750A4)
            )

            PermissionItemCard(
                title = "Contacts Permission",
                desc = "Yaluwanta lesiyenma videos share karanna contacts support eka labagannava.",
                isGranted = hasContacts,
                icon = Icons.Default.Person,
                accentColor = Color(0xFF6750A4)
            )
        }

        // Bottom Beautiful Button
        Button(
            onClick = {
                // Request Standard Permissions first
                val permissionsToRequest = mutableListOf(Manifest.permission.READ_CONTACTS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

                // If API 30+, we also need MANAGE_EXTERNAL_STORAGE which requires Settings Activity
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    Toast.makeText(
                        context, 
                        "Machan, 'All Files' toggle eka active karala permission allow karanna!", 
                        Toast.LENGTH_LONG
                    ).show()
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        context.startActivity(intent)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("grant_permissions_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimary
            ),
            shape = RoundedCornerShape(100), // Rounded pill shape
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Check Mark Icon",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Permissions Allow Karanna",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    icon: ImageVector,
    accentColor: Color
) {
    val statusColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF2ECC71) else Color(0xFFFF5252),
        label = "status_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ThemePrimaryLight),
        border = BorderStroke(1.dp, ThemeBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ThemePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ThemeTextSecondary,
                        lineHeight = 15.sp,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Beautiful status container with text badge + animated icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Text Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "Success" else "Required",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }

                // Beautifully animated Status Icon (✅ or ❎)
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isGranted,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Allowed",
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isGranted,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Required",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGridHomeScreen() {
    val context = LocalContext.current

    // Create CP Hub empty folder inside external/internal storage on launch
    LaunchedEffect(Unit) {
        try {
            val cpHubDir = java.io.File(Environment.getExternalStorageDirectory(), "CP Hub")
            if (!cpHubDir.exists()) {
                cpHubDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val videoList = remember { mutableStateListOf<VideoItem>() }
    
    // Active playback selection
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

    // Load local videos from the assets folder 'vids'
    LaunchedEffect(Unit) {
        try {
            val assetManager = context.assets
            val files = assetManager.list("vids") ?: emptyArray()
            val videoExtensions = listOf(".mp4", ".mkv", ".3gp", ".webm", ".mov")
            val videoFiles = files.filter { file ->
                videoExtensions.any { ext -> file.endsWith(ext, ignoreCase = true) }
            }

            videoList.clear()
            videoFiles.forEachIndexed { index, fileName ->
                val title = if (fileName.contains(".")) {
                    fileName.substringBeforeLast(".")
                } else {
                    fileName
                }
                videoList.add(
                    VideoItem(
                        id = (index + 1).toString(),
                        title = title,
                        videoUrl = "file:///android_asset/vids/$fileName",
                        thumbnailUrl = null,
                        isLocal = true,
                        duration = "Local"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // App Header Bar & Grid Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ThemePrimary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "V Hub",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeTextPrimary
                            )
                        )
                        Text(
                            text = "Vede Lesiyen Karamu",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Video Grid Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Obe Videos (${videoList.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary
                    )
                )
            }

            if (videoList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp)
                        .background(Color.White, shape = RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, ThemeBorderLight), shape = RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(ThemePrimaryLight, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Empty Folder",
                                tint = ThemePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "vids folder eka empty machan!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeTextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Oyaage video files 'assets/vids' folder ekata dala mehi pradarshanaya karanna.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ThemeTextSecondary,
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Beautiful Grid View showing exactly 4 videos per row
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videoList) { video ->
                        VideoThumbnailItem(
                            video = video,
                            onClick = { selectedVideo = video }
                        )
                    }
                }
            }
        }

        // Bottom Bar with Load More Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeBottomBg)
                .border(BorderStroke(1.dp, ThemeBorder), shape = RoundedCornerShape(0.dp))
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // Do nothing for now as requested
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("upload_video_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Load More Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Load More...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
            Text(
                text = "DHEN ALUTH EKAK ADD KARAMU",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ThemeTextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }

    // Video Player Overlay Modal Dialog
    selectedVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            videoList = videoList,
            onVideoSelect = { selectedVideo = it },
            onDismiss = { selectedVideo = null }
        )
    }
}

@Composable
fun VideoThumbnailItem(
    video: VideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square aspect ratio per the Design HTML style specification
            .clickable(onClick = onClick)
            .testTag("video_item_${video.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E1E5)),
        border = BorderStroke(1.dp, ThemeBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (video.isLocal || video.thumbnailUrl == null) {
                // Custom fallback for local videos or missing thumbnails
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(ThemePrimaryLight, ThemeBorder)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ThemePrimary.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Local Video Indicator",
                            tint = ThemePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Play Accent Overlay Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Duration Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.duration,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(
    video: VideoItem,
    videoList: List<VideoItem>,
    onVideoSelect: (VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    var isFullscreen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isVideoPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Sync isFullscreen with the actual physical landscape orientation of the device
    LaunchedEffect(isLandscape) {
        if (isLandscape && !isFullscreen) {
            isFullscreen = true
        }
    }

    // Reset orientation back to standard when the dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Handle manual fullscreen toggle via button clicks
    val onFullscreenToggle = {
        val targetFullscreen = !isFullscreen
        isFullscreen = targetFullscreen
        try {
            if (targetFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dynamic sizing of the controls based on screen orientation to prevent overlap in landscape
    val playPauseSize = if (isLandscape) 48.dp else 64.dp
    val playPauseIconSize = if (isLandscape) 28.dp else 36.dp
    val navButtonSize = if (isLandscape) 36.dp else 48.dp
    val navIconSize = if (isLandscape) 18.dp else 24.dp

    // Toggle auto-hide timer for controls
    LaunchedEffect(controlsVisible, isVideoPlaying) {
        if (controlsVisible && isVideoPlaying) {
            kotlinx.coroutines.delay(4000)
            controlsVisible = false
        }
    }

    // Coroutine loop to update progress indicator
    LaunchedEffect(video, isVideoPlaying) {
        while (true) {
            videoViewInstance?.let { vv ->
                try {
                    currentPos = vv.currentPosition.toLong()
                    val totalDuration = vv.duration.toLong()
                    if (totalDuration > 0) {
                        duration = totalDuration
                    }
                } catch (e: Exception) {
                    // Ignore errors during state transition
                }
            }
            kotlinx.coroutines.delay(250)
        }
    }

    // Handle play/pause commands
    LaunchedEffect(isVideoPlaying) {
        videoViewInstance?.let { vv ->
            try {
                if (isVideoPlaying) {
                    if (!vv.isPlaying) vv.start()
                } else {
                    if (vv.isPlaying) vv.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Handle mute state commands
    LaunchedEffect(isMuted, mediaPlayerInstance) {
        mediaPlayerInstance?.let { mp ->
            try {
                if (isMuted) {
                    mp.setVolume(0f, 0f)
                } else {
                    mp.setVolume(1f, 1f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Format helper function for display timestamp (mm:ss)
    fun formatTime(ms: Long): String {
        val totalSeconds = maxOf(0L, ms / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // Index calculation for Prev and Next buttons
    val currentIndex = videoList.indexOfFirst { it.id == video.id }
    val hasPrev = videoList.size > 1
    val hasNext = videoList.size > 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = !isFullscreen
        )
    ) {
        Card(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .testTag("video_player_dialog")
            },
            shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeBg),
            border = if (isFullscreen) null else BorderStroke(1.dp, ThemeBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = if (isFullscreen) Modifier.fillMaxSize() else Modifier.padding(16.dp)
            ) {
                // Dialog Title Header (Only visible if not in Fullscreen)
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (video.isLocal) "Local Video File" else "Streaming Video Source",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ThemeTextSecondary
                                )
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(ThemePrimary.copy(alpha = 0.12f), shape = CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close player dialog",
                                tint = ThemePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Video Player Container
                Box(
                    modifier = if (isFullscreen) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.77f) // Landscape cinematic 16:9
                            .background(Color.Black, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    },
                    contentAlignment = Alignment.Center
                ) {
                    // Re-create player whenever the video item changes to ensure a pristine state.
                    key(video) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    // Complete custom implementation overlaying controllers in Compose,
                                    // so we do not use native media controller.
                                    setMediaController(null)
                                    
                                    val uri = if (video.videoUrl.startsWith("file:///android_asset/")) {
                                        try {
                                            val assetPath = video.videoUrl.substringAfter("file:///android_asset/")
                                            val cacheFile = java.io.File(ctx.cacheDir, java.io.File(assetPath).name)
                                            if (!cacheFile.exists()) {
                                                ctx.assets.open(assetPath).use { inputStream ->
                                                    java.io.FileOutputStream(cacheFile).use { outputStream ->
                                                        inputStream.copyTo(outputStream)
                                                    }
                                                }
                                            }
                                            Uri.fromFile(cacheFile)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Uri.parse(video.videoUrl)
                                        }
                                    } else {
                                        Uri.parse(video.videoUrl)
                                    }
                                    
                                    setVideoURI(uri)
                                    
                                    setOnPreparedListener { mp ->
                                        isLoading = false
                                        mediaPlayerInstance = mp
                                        
                                        // Set volume according to the muted state
                                        if (isMuted) {
                                            mp.setVolume(0f, 0f)
                                        } else {
                                            mp.setVolume(1f, 1f)
                                        }
                                        
                                        duration = duration.toLong()
                                        if (isVideoPlaying) {
                                            start()
                                        }
                                    }
                                    
                                    setOnCompletionListener {
                                        // Auto-play next or loop
                                        if (hasNext && currentIndex != -1) {
                                            val nextIndex = if (currentIndex < videoList.size - 1) currentIndex + 1 else 0
                                            onVideoSelect(videoList[nextIndex])
                                        } else {
                                            isVideoPlaying = false
                                        }
                                    }
                                    
                                    setOnErrorListener { _, _, _ ->
                                        isLoading = false
                                        hasError = true
                                        true
                                    }
                                    
                                    videoViewInstance = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    controlsVisible = !controlsVisible
                                }
                        )
                    }

                    // Loading Spinner Overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ThemePrimary,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    // Error Feedback Overlay
                    if (hasError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Playback error icon",
                                    tint = Color.Red,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Machan, video eka play karanna ba!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Internet connect velada kiyala check karanna.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Custom Overlay controls (inspired by YouTube premium interface overlay)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible && !isLoading && !hasError,
                        enter = fadeIn(animationSpec = spring()),
                        exit = fadeOut(animationSpec = spring())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { controlsVisible = false }
                        ) {
                            // Close/Back button in Fullscreen mode inside the overlay
                            if (isFullscreen) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopStart)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            isFullscreen = false
                                            try {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Exit Fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Center Controller Action Row (Prev, Play/Pause, Next)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Video Button
                                if (hasPrev && currentIndex != -1) {
                                    IconButton(
                                        onClick = {
                                            val prevIndex = if (currentIndex > 0) currentIndex - 1 else videoList.size - 1
                                            onVideoSelect(videoList[prevIndex])
                                        },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                            .size(navButtonSize)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Previous Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(navIconSize)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(if (isLandscape) 16.dp else 24.dp))
                                }

                                // Play / Pause Button
                                IconButton(
                                    onClick = { isVideoPlaying = !isVideoPlaying },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                                        .size(playPauseSize)
                                ) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(playPauseIconSize)
                                    )
                                }

                                // Next Video Button
                                if (hasNext && currentIndex != -1) {
                                    Spacer(modifier = Modifier.width(if (isLandscape) 16.dp else 24.dp))
                                    IconButton(
                                        onClick = {
                                            val nextIndex = if (currentIndex < videoList.size - 1) currentIndex + 1 else 0
                                            onVideoSelect(videoList[nextIndex])
                                        },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                            .size(navButtonSize)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Next Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(navIconSize)
                                        )
                                    }
                                }
                            }

                             // Bottom Controls (Seek bar / Custom Canvas Timeline, Mute, Timestamps, Fullscreen)
                             Column(
                                 modifier = Modifier
                                     .align(Alignment.BottomCenter)
                                     .fillMaxWidth()
                                     .background(
                                         Brush.verticalGradient(
                                             colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                         )
                                     )
                                     .padding(horizontal = 12.dp)
                                     .padding(bottom = 6.dp, top = 16.dp)
                             ) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(horizontal = 4.dp),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     // Left: Mute/Unmute
                                     IconButton(
                                         onClick = { isMuted = !isMuted },
                                         modifier = Modifier.size(36.dp)
                                     ) {
                                         Icon(
                                             imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                             contentDescription = if (isMuted) "Unmute" else "Mute",
                                             tint = Color.White,
                                             modifier = Modifier.size(22.dp)
                                         )
                                     }
 
                                     // Right: Fullscreen Toggle (Above the timeline, on the bottom right)
                                     IconButton(
                                         onClick = { onFullscreenToggle() },
                                         modifier = Modifier.size(36.dp)
                                     ) {
                                         Icon(
                                             imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                             contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                             tint = Color.White,
                                             modifier = Modifier.size(24.dp)
                                         )
                                     }
                                 }
 
                                 Spacer(modifier = Modifier.height(2.dp))
 
                                 // Timestamp & Custom Canvas Progress slider row (At the very bottom)
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(
                                         text = formatTime(currentPos),
                                         style = MaterialTheme.typography.labelSmall.copy(
                                             color = Color.White,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 11.sp
                                         ),
                                         modifier = Modifier.padding(horizontal = 6.dp)
                                     )
 
                                     // Custom Sleek Seek Bar
                                     BoxWithConstraints(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(24.dp)
                                     ) {
                                         val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
                                         val progressFraction = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f
                                         val activeColor = ThemePrimaryLight
                                         val inactiveColor = Color.White.copy(alpha = 0.3f)
 
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .pointerInput(duration, widthPx) {
                                                     detectTapGestures { offset ->
                                                        if (duration > 0) {
                                                            val progress = (offset.x / widthPx).coerceIn(0f, 1f)
                                                            val targetPos = (progress * duration).toLong()
                                                            currentPos = targetPos
                                                            videoViewInstance?.seekTo(targetPos.toInt())
                                                        }
                                                     }
                                                 }
                                                 .pointerInput(duration, widthPx) {
                                                     detectHorizontalDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        if (duration > 0) {
                                                            val progress = (change.position.x / widthPx).coerceIn(0f, 1f)
                                                            val targetPos = (progress * duration).toLong()
                                                            currentPos = targetPos
                                                            videoViewInstance?.seekTo(targetPos.toInt())
                                                        }
                                                     }
                                                 }
                                         ) {
                                             Canvas(modifier = Modifier.fillMaxSize()) {
                                                 val cy = size.height / 2f
                                                 val trackHeight = 4.dp.toPx()
                                                 val activeWidth = size.width * progressFraction
                                                 
                                                 // Inactive track (grey background)
                                                 drawLine(
                                                     color = inactiveColor,
                                                     start = Offset(0f, cy),
                                                     end = Offset(size.width, cy),
                                                     strokeWidth = trackHeight,
                                                     cap = StrokeCap.Round
                                                 )
                                                 
                                                 // Active track (primary progress)
                                                 drawLine(
                                                     color = activeColor,
                                                     start = Offset(0f, cy),
                                                     end = Offset(activeWidth, cy),
                                                     strokeWidth = trackHeight,
                                                     cap = StrokeCap.Round
                                                 )
                                                 
                                                 // Thumb (small elegant circle indicator)
                                                 drawCircle(
                                                     color = activeColor,
                                                     radius = 6.dp.toPx(),
                                                     center = Offset(activeWidth, cy)
                                                 )
                                             }
                                         }
                                     }
 
                                     Text(
                                         text = formatTime(duration),
                                         style = MaterialTheme.typography.labelSmall.copy(
                                             color = Color.White,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 11.sp
                                         ),
                                         modifier = Modifier.padding(horizontal = 6.dp)
                                     )
                                 }
                             }
                        }
                    }
                }

                // Close / Info prompt (Only shown when not in Fullscreen)
                if (!isFullscreen) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💡 Machan, YouTube eke vage controls up-to-down display karanna video eka click karanna. Slider eken track eka seek karanna puluvan, prev/next ekka video select karanna puluvan.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemeTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}
