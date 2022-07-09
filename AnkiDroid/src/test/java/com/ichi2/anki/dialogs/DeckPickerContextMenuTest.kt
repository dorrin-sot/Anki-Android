/*
 *  Copyright (c) 2022 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.dialogs

import android.content.Intent
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.decks.deckId
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.DeckPickerContextMenu.Companion.instance
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.testutils.assertThrows
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DeckPickerContextMenuTest : RobolectricTest() {
    @Test
    fun ensure_cannot_be_instantiated_without_arguments() {
        assertThrows<IllegalStateException> { DeckPickerContextMenu(col).deckId }
    }

    @Test
    fun testBrowseCards() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 0)

            val browser = shadowOf(this).nextStartedActivity!!
            assertEquals("com.ichi2.anki.CardBrowser", browser.component!!.className)

            assertEquals(deckId, col.decks.selected())
        }
    }

    @Test
    fun testRenameDeck() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 1)

            CreateDeckDialog.instance!!.renameDeck("Deck 2")
            assertEquals("Deck 2", col.decks.name(deckId))
        }
    }

    @Test
    fun testCreateSubdeck() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 2)

            CreateDeckDialog.instance!!.createSubDeck(deckId, "Deck 2")
            assertThat(col.decks.allNames(), containsInAnyOrder("Default", "Deck 1", "Deck 1::Deck 2"))
        }
    }

    @Test
    fun testShowDeckOptions() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 3)

            val deckOptions = shadowOf(this).nextStartedActivity!!
            assertEquals("com.ichi2.anki.DeckOptions", deckOptions.component!!.className)
            assertEquals(deckId, deckOptions.getLongExtra("did", 1))
        }
    }

    @Test
    fun testDeleteDeck() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 7)

            assertThat(col.decks.allNames(), contains("Default"))
        }
    }

    @Test
    fun testCreateShortcut() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 6)

            assertEquals(
                "Deck 1",
                ShortcutManagerCompat.getShortcuts(this, FLAG_MATCH_PINNED).first().shortLabel
            )
        }
    }

    @Test
    fun testUnbury() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            col.models.byName("Basic")!!.put("did", deckId)
            val card = addNoteUsingBasicModel("front", "back").firstCard()
            col.sched.buryCards(longArrayOf(card.id))
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            assertTrue(col.sched.haveBuried(deckId))

            openContextMenuAndSelectItem(recyclerView, 6)

            assertFalse(col.sched.haveBuried(deckId))
        }
    }

    @Test
    fun testCustomStudy() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 4)

            assertNotNull(CustomStudyDialog.instance)
            assertEquals(deckId, CustomStudyDialog.instance!!.requireArguments().getLong("did"))
        }
    }

    @Test
    fun testExportDeck() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val deckId = addDeck("Deck 1")
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 5)

            assertNotNull(ExportDialog.instance)
            assertEquals(deckId, ExportDialog.instance!!.requireArguments().getLong("did"))
        }
    }

    @Test
    fun testDynRebuildAndEmpty() {
        startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).run {
            val cardIds = (0..3)
                .map { addNoteUsingBasicModel("$it", "").firstCard().id }
            assertTrue(allCardsInSameDeck(cardIds, 1))
            val deckId = addDynamicDeck("Deck 1")
            col.sched.rebuildDyn(deckId)
            assertTrue(allCardsInSameDeck(cardIds, deckId))
            updateDeckList()
            assertEquals(1, visibleDeckCount)

            openContextMenuAndSelectItem(recyclerView, 2) // Empty

            assertTrue(allCardsInSameDeck(cardIds, 1))

            openContextMenuAndSelectItem(recyclerView, 1) // Rebuild

            assertTrue(allCardsInSameDeck(cardIds, deckId))
        }
    }

    private fun allCardsInSameDeck(cardIds: List<Long>, deckId: Long): Boolean =
        cardIds.all { col.getCard(it).did == deckId }

    private fun openContextMenuAndSelectItem(contextMenu: RecyclerView, index: Int) {
        contextMenu.postDelayed({
            contextMenu.findViewHolderForAdapterPosition(0)!!
                .itemView.performLongClick()

            val dialogRecyclerView = instance!!.mRecyclerView!!

            dialogRecyclerView.apply {
                scrollToPosition(index)
                postDelayed({
                    findViewHolderForAdapterPosition(index)!!
                        .itemView.performClick()
                }, 1)
            }
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        }, 1)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
