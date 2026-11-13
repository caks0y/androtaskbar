package com.example

import android.content.Context
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import com.example.data.SafFileExplorerRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Windroid", appName)
  }


  @Test
  fun `quick storage picker uri is a tree uri for selected folder`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val uri = SafFileExplorerRepository(context).quickSafInitialUri("Download")

    assertEquals("primary:Download", DocumentsContract.getTreeDocumentId(uri))
  }
}
