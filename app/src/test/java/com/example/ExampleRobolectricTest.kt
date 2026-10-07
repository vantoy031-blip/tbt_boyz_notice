package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Notice
import com.example.data.model.NoticeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches TBT BOYz Notice`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TBT BOYz Notice", appName)
  }

  @Test
  fun `notice domain category parsing works correctly`() {
    val notice = Notice(
      id = "test-1",
      title = "Test Important Notice",
      description = "Detailed notice description for testing.",
      category = "important",
      isImportant = true,
      isPinned = true,
      isArchived = false,
      createdAt = 1000L,
      updatedAt = 1000L,
      publishedAt = 1000L
    )

    assertEquals(NoticeCategory.IMPORTANT, notice.categoryEnum)
    assertTrue(notice.isImportant)
    assertTrue(notice.isPinned)
    assertFalse(notice.isArchived)
  }
}
