package com.mobprog.lokalert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

@Composable
fun SearchScreen() {
    Column {
        SearchSection()
        MapSection()
        RecentSearchSection()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection(
    // Removed onPlacePinClick since the FAB handles this now
    onSearch: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var userHasSelectedSuggestion by remember { mutableStateOf(false) }


    // Places Client Setup
    val placesClient = remember {
        if (Places.isInitialized()) Places.createClient(context) else null
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    // Autocomplete Logic
    LaunchedEffect(searchText) {
        if (userHasSelectedSuggestion) {
            userHasSelectedSuggestion = false
            return@LaunchedEffect
        }
        if (searchText.isNotEmpty() && placesClient != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("PH")
                .setSessionToken(token)
                .setQuery(searchText)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                    expanded = suggestions.isNotEmpty()
                }
                .addOnFailureListener {
                    suggestions = emptyList()
                    expanded = false
                }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

    // UI: A Floating Card Style
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Outer padding from screen edges
            .padding(top = 32.dp) // Push down from status bar
    ) {
        // We use a Surface to give it a solid background (White/Dark) and shadow (Elevation)
        Surface(
            shadowElevation = 8.dp, // Adds the shadow
            shape = RoundedCornerShape(28.dp), // Fully rounded corners
            color = MaterialTheme.colorScheme.surface, // Solid background color
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // The Search Text Field
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = { Text("Search locations...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            // Clear button
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.clickable {
                                    searchText = ""
                                    expanded = false
                                }
                            )
                        }
                    },
                    // Transparent colors so the Surface color shows through
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch?.invoke(searchText)
                            expanded = false
                        }
                    )
                )

                // The Suggestions List (Integrated directly below search)
                // We use AnimatedVisibility so it slides down nicely
                androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userHasSelectedSuggestion = true
                                        searchText = suggestion
                                        expanded = false
                                        onSuggestionClick?.invoke(suggestion)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1C2A38), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Map Placeholder",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RecentSearchSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recent Searches", fontWeight = FontWeight.Bold)
        }
        Text(
            text = "No recent searches",
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}