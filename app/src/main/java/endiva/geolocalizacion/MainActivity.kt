package endiva.geolocalizacion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import endiva.geolocalizacion.ui.theme.GeolocalizacionTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeolocalizacionTheme {
                LocationScreen()
            }
        }
    }
}

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    var lat by remember { mutableStateOf<Double?>(null) }
    var lon by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Float?>(null) }

    var info by remember { mutableStateOf("Pulsa el botón para comprobar permisos y obtener ubicación") }
    val fusedClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun checkPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = fineGranted || coarseGranted

        info = if (hasPermission) {
            "Permiso concedido. Recibiendo ubicación..."
        } else {
            "Permiso denegado. No se puede obtener la ubicación."
        }
    }
    val request = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()
    }
    val callback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lat = location.latitude
                lon = location.longitude
                accuracy = location.accuracy
                info = "Ubicación actualizada"
            }
        }
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            try {
                fusedClient.requestLocationUpdates(
                    request,
                    callback,
                    context.mainLooper
                )
            } catch (e: SecurityException) {
                info = "Error de permisos: ${e.message}"
            }
        }

        onDispose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    fun startLocation() {
        if (!hasPermission) {
            info = "Sin permisos de ubicación"
            return
        }

        info = "Intentando obtener ubicación..."

        try {
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                        accuracy = location.accuracy
                        info = "Ubicación obtenida (lastLocation)"
                    } else {
                        info = "Sin última ubicación, solicitando actualizaciones..."
                        fusedClient.requestLocationUpdates(
                            request,
                            callback,
                            context.mainLooper
                        )
                    }
                }
                .addOnFailureListener {
                    info = "Error obteniendo lastLocation"
                }
        } catch (e: SecurityException) {
            info = "Error de permisos: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Geolocalización", style = MaterialTheme.typography.titleLarge)

        Text(info)

        Button(onClick = {
            hasPermission = checkPermission()
            if (!hasPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                info = "Permiso ya concedido. Recibiendo ubicación..."
            }
        }) {
            Text("Activar ubicación")
        }

        Text("Latitud: ${lat ?: "-"}")
        Text("Longitud: ${lon ?: "-"}")
        Text("Precisión (m): ${accuracy ?: "-"}")
    }
}

