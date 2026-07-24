package com.sunilshetty.clearspace.ui.components

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.sunilshetty.clearspace.R

@Composable
fun InfinityLoaderGif(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(R.drawable.infinity_loader)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
