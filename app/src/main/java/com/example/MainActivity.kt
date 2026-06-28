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
    
    // Permission States
    var hasContactsPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    
    // Main UI transition state based on permissions
    var showPermissionPage by remember { mutableStateOf(true) }

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

        // We require all three permissions to navigate to the video home screen
        showPermissionPage = !(hasContactsPermission && hasNotificationPermission && hasStoragePermission)
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

            // Beautifully animated Status Icon (✅ or ❎)
            Box(
                modifier = Modifier.size(36.dp),
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
                        modifier = Modifier.size(30.dp)
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
                        modifier = Modifier.size(30.dp)
                    )
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
    
    // Default pre-loaded test videos from high-quality public repositories
    val defaultVideos = remember {
        listOf(
            VideoItem("1", "Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg", duration = "9:56"),
            VideoItem("2", "Elephant's Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg", duration = "10:53"),
            VideoItem("3", "For Bigger Blazes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg", duration = "0:15"),
            VideoItem("4", "For Bigger Escapes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg", duration = "0:15"),
            VideoItem("5", "For Bigger Fun", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerFun.jpg", duration = "0:15"),
            VideoItem("6", "For Bigger Joyrides", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg", duration = "0:15"),
            VideoItem("7", "For Bigger Meltdowns", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerMeltdowns.jpg", duration = "0:15"),
            VideoItem("8", "Sintel Test Walk", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg", duration = "0:52"),
            VideoItem("9", "Subaru Outback", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/SubaruOutbackOnStreetAndDirt.jpg", duration = "9:54"),
            VideoItem("10", "Tears of Steel", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg", duration = "12:14"),
            VideoItem("11", "We Are Going", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/WeAreGoingOnBullrun.jpg", duration = "0:47"),
            VideoItem("12", "What Car Can", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/WhatCarCanYouGetForAGrand.jpg", duration = "9:45"),
            VideoItem("13", "Forest Flyover", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg", duration = "4:12"),
            VideoItem("14", "Deep Blue Water", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg", duration = "5:18"),
            VideoItem("15", "Sunset Cinematic", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg", duration = "2:30"),
            VideoItem("16", "Metro Expressway", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg", duration = "1:45"),
            VideoItem("17", "Nature Wildlife", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerFun.jpg", duration = "8:22"),
            VideoItem("18", "Neon Cyber City", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg", duration = "3:55"),
            VideoItem("19", "Abstract Shapes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerMeltdowns.jpg", duration = "0:15"),
            VideoItem("20", "Sintel Flight", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg", duration = "0:15")
        )
    }

    // Dynamic Video List (starts with default 20)
    val videoList = remember { mutableStateListOf<VideoItem>().apply { addAll(defaultVideos) } }
    
    // Active playback selection
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

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
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("video_player_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeBg),
            border = BorderStroke(1.dp, ThemeBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Dialog Title Header
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

                // Android Native Video Player Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.77f) // Landscape cinematic 16:9
                        .background(Color.Black, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                val mediaController = MediaController(context)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setVideoURI(Uri.parse(video.videoUrl))
                                
                                setOnPreparedListener { mp ->
                                    isLoading = false
                                    mp.start() // Auto start playback
                                }
                                
                                setOnErrorListener { _, _, _ ->
                                    isLoading = false
                                    hasError = true
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fun prompt in Singlish
                Text(
                    text = "💡 Machan, video eka pause karanna nathnam track eka seek karanna video frame eka uda click karanna control bars labaganna.",
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
