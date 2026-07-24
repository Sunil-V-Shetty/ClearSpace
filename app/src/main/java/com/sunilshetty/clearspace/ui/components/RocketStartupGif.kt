package com.sunilshetty.clearspace.ui.components

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
private fun AssetGif(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
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
            .data(drawableRes)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
        modifier = modifier.size(size),
    )
}

@Composable
fun RocketStartupGif(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    contentDescription: String? = null,
) {
    AssetGif(
        drawableRes = R.drawable.rocket_startup,
        modifier = modifier,
        size = size,
        contentDescription = contentDescription,
    )
}

@Composable
fun CompleteGif(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    contentDescription: String? = null,
) {
    AssetGif(
        drawableRes = R.drawable.complete,
        modifier = modifier,
        size = size,
        contentDescription = contentDescription,
    )
}
