package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val user = viewModel.currentUser
    val scrollState = rememberScrollState()

    var isEditing by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(user.name) }
    var editEmail by remember { mutableStateOf(user.email) }
    var editPhone by remember { mutableStateOf(user.phone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Foydalanuvchi Profili", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.DiscoverMap) }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        modifier = modifier
    ) { padValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card carrying Avatar, Name & current Role
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar Simulation
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBox,
                            contentDescription = "Avatar shape",
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = user.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(10.dp))

                    // Role indicator tag
                    Box(
                        modifier = Modifier
                            .background(
                                if (user.role == "ADMIN") Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (user.role == "ADMIN") "Rol: Tizim Administratori" else "Rol: Fuqaro (Citizen)",
                            color = if (user.role == "ADMIN") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // High Value Role Config Switcher (Crucial for live exploration of all features!)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Rol almashtirgich (Dastur tahlili):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ushbu tugma orqali oddiy Fuqaro (Hisobot yuborish) yoki Admin boshqaruv paneli (Muammolarni tahlil qilish, statuslarni o'zgartirish) rejimlariga o'ting.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.switchUserRole("USER") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.role == "USER") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                contentColor = if (user.role == "USER") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_switch_user_btn")
                        ) {
                            Text(text = "Fuqaro Ko'rinishi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.switchUserRole("ADMIN") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.role == "ADMIN") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                contentColor = if (user.role == "ADMIN") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_switch_admin_btn")
                        ) {
                            Text(text = "Admin Ko'rinishi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Profile fields form list
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Shaxsiy Ma'lumotlar", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        TextButton(
                            onClick = {
                                if (isEditing) {
                                    viewModel.updateProfile(editName, editEmail, editPhone)
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.testTag("set_edit_profile_btn")
                        ) {
                            Text(text = if (isEditing) "Saqlash" else "Tahrirlash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(text = "To'liq ism-familiya") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text(text = "Elektron pochta") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_profile_email"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text(text = "Telefon raqami") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone"),
                            singleLine = true
                        )
                    } else {
                        ProfileInfoItem(label = "F.I.SH", value = user.name, icon = Icons.Filled.Person)
                        ProfileInfoItem(label = "Elektron pochta", value = user.email, icon = Icons.Filled.Email)
                        ProfileInfoItem(label = "Aloqa raqami", value = user.phone, icon = Icons.Filled.Phone)
                    }
                }
            }

            // Application Settings or general utilities shortcuts
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Dastur Sozlamalari", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Admin Dashboard direct jump button (only displayed if current user is Admin after role switches!)
                    if (user.role == "ADMIN") {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goto_admin_dash_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.Info, contentDescription = "Dashboard link")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Admin Boshqaruv Paneliga O'tish", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    ProfileNavigationLink(label = "Tilni tanlash (O'zbekcha)", icon = Icons.Filled.List)
                    ProfileNavigationLink(label = "Shaxsiy xavfsizlik va parool", icon = Icons.Filled.Warning)
                    ProfileNavigationLink(label = "Muammo Xaritasi Qoidalari", icon = Icons.Filled.Warning)
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProfileNavigationLink(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "Right direction link", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
    }
}
