package com.sunilshetty.clearspace.data.repository

import com.sunilshetty.clearspace.data.local.DeviceStorageDataSource
import com.sunilshetty.clearspace.data.model.DeviceStorageInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStorageRepository @Inject constructor(
    private val deviceStorageDataSource: DeviceStorageDataSource,
) {
    fun getDeviceStorage(): DeviceStorageInfo = deviceStorageDataSource.readInternalStorage()
}
