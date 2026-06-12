package com.example.ui.components

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.LocalProblem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Composable
fun UzbekistanInteractiveMap(
    problems: List<LocalProblem>,
    onProblemSelected: (Int) -> Unit,
    onCoordinatePicked: (Double, Double) -> Unit,
    pickingCoordinates: Boolean = false,
    pickLat: Double = 41.311081,
    pickLng: Double = 69.279737,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapLoaded by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Convert problems list to lightweight JSON for leaf JS
    val problemsJson = remember(problems) {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(List::class.java)
        
        val listMap = problems.map {
            mapOf(
                "id" to it.id,
                "title" to it.title,
                "categoryName" to it.categoryName,
                "status" to it.status,
                "latitude" to it.latitude,
                "longitude" to it.longitude
            )
        }
        adapter.toJson(listMap) ?: "[]"
    }

    // Effect to update map markers when problem lists or picker indices fluctuate
    LaunchedEffect(problemsJson, isMapLoaded, webViewInstance) {
        if (isMapLoaded && webViewInstance != null) {
            webViewInstance?.post {
                webViewInstance?.evaluateJavascript("setMarkers('$problemsJson')", null)
            }
        }
    }

    // Effect to update picker coordinates position
    LaunchedEffect(pickLat, pickLng, pickingCoordinates, isMapLoaded, webViewInstance) {
        if (isMapLoaded && webViewInstance != null && pickingCoordinates) {
            webViewInstance?.post {
                webViewInstance?.evaluateJavascript("updatePickPoint($pickLat, $pickLng)", null)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }

                    // Native Javascript Interaction bridge
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMarkerClick(id: Int) {
                            onProblemSelected(id)
                        }

                        @JavascriptInterface
                        fun onMapClick(lat: Double, lng: Double) {
                            onCoordinatePicked(lat, lng)
                        }
                    }, "Android")

                    // Embedded Interactive Leaflet HTML Content
                    val rawHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                            <style>
                                html, body, #map {
                                    width: 100%; height: 100%; margin: 0; padding: 0; background: #121212;
                                }
                                .leaflet-container { background: #121212 !important; }
                                .leaflet-popup-content-wrapper {
                                    background: #252528; color: #ffffff; font-family: sans-serif; border-radius: 8px;
                                }
                                .leaflet-popup-tip { background: #252528; }
                            </style>
                        </head>
                        <body>
                            <div id="map"></div>
                            <script>
                                var map = L.map('map', { zoomControl: false }).setView([41.311081, 69.279737], 12);
                                
                                // Beautiful CartoDB Dark Premium Map Styles matching Jetpack themes
                                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                                    attribution: 'Muammo Xaritasi © OpenStreetMap'
                                }).addTo(map);

                                var markersGroup = L.layerGroup().addTo(map);
                                var pickMarker = null;

                                function setMarkers(problemsStr) {
                                    markersGroup.clearLayers();
                                    try {
                                        var problems = JSON.parse(problemsStr);
                                        problems.forEach(function(p) {
                                            var markerColor = "#ff9800"; // Default orange
                                            if (p.status === "ACCEPTED") markerColor = "#fdd835"; // yellow
                                            if (p.status === "IN_PROGRESS") markerColor = "#2196f3"; // blue
                                            if (p.status === "RESOLVED") markerColor = "#4caf50"; // green

                                            var iconSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 24 24" fill="${'$'}markerColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/><circle cx="12" cy="9" r="3.5" fill="#ffffff"/></svg>`;
                                            
                                            var customIcon = L.divIcon({
                                                html: iconSvg,
                                                iconSize: [30, 30],
                                                iconAnchor: [15, 30]
                                            });

                                            var marker = L.marker([p.latitude, p.longitude], {icon: customIcon}).addTo(markersGroup);
                                            marker.bindPopup(`
                                                <div style="font-size:12px; line-height:1.4;">
                                                    <b style="font-size:14px; color:#64B5F6;">${'$'}{p.title}</b><br/>
                                                    <span style="font-style:italic; color:#B0BEC5;">Category: ${'$'}{p.categoryName}</span><br/>
                                                    <span style="font-weight:bold; color:${'$'}markerColor">Holati: ${'$'}{p.status}</span><br/>
                                                    <button onclick="Android.onMarkerClick(${'$'}{p.id})" style="margin-top:8px; border:none; width:100%; height:28px; background:${'$'}markerColor; color:#fff; border-radius:4px; font-weight:bold; cursor:pointer;">Kirish</button>
                                                </div>
                                            `);
                                        });
                                    } catch(e) {
                                        console.error("JSON Error in Leaflet", e);
                                    }
                                }

                                map.on('click', function(e) {
                                    Android.onMapClick(e.latlng.lat, e.latlng.lng);
                                });

                                function updatePickPoint(lat, lng) {
                                    if (pickMarker) {
                                        pickMarker.setLatLng([lat, lng]);
                                    } else {
                                        var pickIcon = L.divIcon({
                                            html: `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 24 24" fill="#FF5252"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/><path d="M12 4c-2.76 0-5 2.24-5 5 0 2.88 2.88 7.19 5 9.88 2.11-2.69 5-7 5-9.88 0-2.76-2.24-5-5-5z"/></svg>`,
                                            iconSize: [34, 34],
                                            iconAnchor: [17, 34]
                                        });
                                        pickMarker = L.marker([lat, lng], {icon: pickIcon}).addTo(map);
                                    }
                                    map.setView([lat, lng], 13);
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://leafletjs.com/reference", rawHtml, "text/html", "utf-8", null)
                }
            },
            update = { webView ->
                webViewInstance = webView
            },
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        )

        // Loading Progress indicator over map loading
        if (!isMapLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sun'iy Map Yuklanmoqda...",
                        style = MaterialTheme.styleScheme().bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// Convenient MaterialTheme.typography fallback
@Composable
fun MaterialTheme.styleScheme() = this.typography
