package com.pulselink.beacon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulselink.beacon.model.sampleContacts
import com.pulselink.beacon.model.sampleConversation
import com.pulselink.beacon.model.sampleThreads
import com.pulselink.beacon.ui.InboxScreen
import com.pulselink.beacon.ui.contact.EditContactScreen
import com.pulselink.beacon.ui.message.ChatScreen
import com.pulselink.beacon.ui.settings.SettingsScreen
import com.pulselink.beacon.ui.settings.SettingsHelpScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseLinkBeaconTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "inbox") {
                    composable("inbox") {
                        InboxScreen(
                            threads = sampleThreads,
                            onThreadClick = { thread ->
                                navController.navigate("chat/${thread.id}")
                            },
                            onAvatarClick = { thread ->
                                navController.navigate("edit/${thread.id}")
                            },
                            onTrustedClick = { thread ->
                                navController.navigate("edit/${thread.id}")
                            },
                            onSettings = { navController.navigate("settings") },
                            onBack = { finish() }
                        )
                    }
                    composable(
                        route = "chat/{threadId}",
                        arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val threadId = backStackEntry.arguments?.getString("threadId").orEmpty()
                        val thread = sampleThreads.find { it.id == threadId } ?: sampleThreads.first()
                        val messages = sampleConversation[threadId] ?: sampleConversation.values.first()
                        ChatScreen(
                            title = thread.title,
                            isGroup = threadId == "group",
                            members = listOf("Anna", "Luca", "Mia", "Ivy"),
                            messages = messages,
                            onBack = { navController.popBackStack() },
                            onSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenHelp = { navController.navigate("settings/help") },
                            onOpenFavorites = {
                                val firstId = sampleThreads.firstOrNull()?.id ?: "me"
                                navController.navigate("edit/$firstId")
                            }
                        )
                    }
                    composable("settings/help") {
                        SettingsHelpScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "edit/{threadId}",
                        arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val threadId = backStackEntry.arguments?.getString("threadId").orEmpty()
                        val contact = sampleContacts[threadId] ?: sampleContacts.values.first()
                        EditContactScreen(
                            contact = contact,
                            onBack = { navController.popBackStack() },
                            onSave = { updated ->
                                // In a real app this would persist. For now, just return.
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
