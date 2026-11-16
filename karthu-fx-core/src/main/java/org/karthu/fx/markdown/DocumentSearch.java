package org.karthu.fx.markdown;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import org.karthu.fx.form.search.Mark;
import org.karthu.fx.form.search.PhraseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Markdown view's search at work, beside the rendering, the selection and the zoom: it runs the search text through
 * the document's searchable segments ({@link Searchable}) in document order with the matching rule
 * ({@link PhraseQuery}), keeps the matches and the current one, moves the current one in the search's direction, and
 * has the segments near the visible area mark their matches.
 *
 * <p><b>Order.</b> The matches are numbered from 0 in document order: the segments in the order of the document
 * selection's list, within a segment its texts in its order, within a text from its start.
 *
 * <p><b>Marks near the visible area only.</b> The matches of the whole document are found at once, but only the
 * segments near the visible area ({@link NearArea}) build their marks, at each layout of the view and each scroll: so a
 * one-letter search in a large document costs the scan of the text, not the shapes of tens of thousands of matches. A
 * segment that leaves the area keeps its marks until the search or the current match changes them. The segments lie
 * one below the other in document order, so the first one near the area is found by halving. A long table marks only
 * its rows near the area ({@link Searchable#showSearchMatchesNear(NearArea)}).
 *
 * <p><b>Table changes.</b> A table tells the search when the reader sorts it or moves, hides or shows a column
 * ({@link Searchable#onSearchTextsChanged(Runnable)}): the search runs again and numbers the matches anew, and finds
 * the current match again by what names its text across the change ({@link Searchable#searchTextKey(int)}).
 */
final class DocumentSearch {

    private final StringProperty searchText;
    private final ReadOnlyIntegerWrapper matchCount;
    private final ReadOnlyIntegerWrapper matchIndex;
    private final ObjectProperty<SearchDirection> direction;
    private final DocumentSelection selection;
    private final ScrollPane scrollPane;

    /**
     * The query of the search text.
     */
    private PhraseQuery query = PhraseQuery.of(null);

    /**
     * Every match of the document, in document order.
     */
    private final List<Found> matches = new ArrayList<>();

    /**
     * The segments with matches, in document order.
     */
    private final List<Part> parts = new ArrayList<>();

    /**
     * The segments that show their marks now.
     */
    private final Set<Searchable> marked = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Whether the view has laid out the segments of the shown document: before, their bounds say nothing of where
     * they lie, and no segment builds its marks.
     */
    private boolean laidOut;

    /**
     * Whether a new or changed search waits for the view's first layout of the document to scroll to its current
     * match.
     */
    private boolean scrollPending;

    /**
     * Creates the search of a view's document and runs every change of the search text from now on.
     *
     * @param searchText the view's search text
     * @param matchCount the view's number of matches, which the search sets
     * @param matchIndex the view's index of the current match, which the search sets
     * @param direction  the view's direction of the search
     * @param selection  the document's selection, which knows its segments in document order
     * @param scrollPane the view's scroll pane, whose visible area the marks are built near
     */
    DocumentSearch(StringProperty searchText, ReadOnlyIntegerWrapper matchCount, ReadOnlyIntegerWrapper matchIndex,
                   ObjectProperty<SearchDirection> direction, DocumentSelection selection, ScrollPane scrollPane) {
        this.searchText = searchText;
        this.matchCount = matchCount;
        this.matchIndex = matchIndex;
        this.direction = direction;
        this.selection = selection;
        this.scrollPane = scrollPane;
        searchText.addListener(observable -> searchChanged());
        scrollPane.vvalueProperty().addListener(observable -> markNear());
    }

    /**
     * Runs the search through a new document, the view's segments rendered anew: the current match is chosen as for
     * a new search. The new segments build their marks after the view's next layout.
     */
    void documentChanged() {
        documentChanged(null);
    }

    /**
     * Where the current match lies, noted in the shown document for a new version of its text: its segment, to be
     * found again in the new document ({@link NotedSegment}), what names its text there across a new order of the
     * texts ({@link Searchable#searchTextKey(int)}), and its place in the segment.
     *
     * @return the place, {@code null} without a current match
     */
    Place currentPlace() {
        Found current = currentMatch();
        if (current == null) {
            return null;
        }
        List<Segment> segments = selection.segments();
        Searchable segment = parts.get(current.part()).segment();
        return new Place(NotedSegment.of(segments, segments.get(current.order())),
                segment.searchTextKey(current.match().text()), current.match());
    }

    /**
     * Runs the search through a new document, the view's segments rendered anew. With the place of the old current
     * match, a new version of the same text: the first match at or after that place in the direction becomes current,
     * wrapping, as for a changed search. Without one, the current match is chosen as for a new search. The new
     * segments build their marks after the view's next layout.
     *
     * @param place the old current match's place ({@link #currentPlace()}); {@code null} for another document
     */
    void documentChanged(Place place) {
        // The old segments are discarded with their marks.
        marked.clear();
        laidOut = false;
        // A new text does not scroll to its current match.
        scrollPending = false;
        findAll();
        publish(place == null ? firstInDirection() : nearestInDirection(place));
    }

    /**
     * Where the search goes on from the place of an old current match in a new version of the text: its segment found
     * again, the match's text found again in it by what names it, the match's place in that text. A new table starts
     * unsorted with every column, so the number of the text in a table the reader sorted or rearranged names another
     * cell there; the number counts only where the key names no text of the new segment, as a table edited itself may
     * lack the cell.
     *
     * @return the index of the new current match, -1 without matches
     */
    private int nearestInDirection(Place place) {
        List<Segment> segments = selection.segments();
        Segment segment = place.segment().findIn(segments);
        if (segment == null) {
            return firstInDirection();
        }
        int order = 0;
        while (segments.get(order) != segment) {
            order++;
        }
        Searchable.Match match = place.match();
        int text = segment instanceof Searchable searchable ? searchable.searchTextNumber(place.textKey()) : -1;
        if (text >= 0) {
            match = new Searchable.Match(text, match.start(), match.end());
        }
        // A match of no segment with matches: only its order and its range count for where it starts.
        return nearestInDirection(new Found(-1, order, match));
    }

    /**
     * Builds the marks near the visible area once the view has laid out its document. The view calls it at the end of
     * each of its layouts; the scroll pane lays the document out first if it has not yet, so the segments are found
     * where they lie.
     */
    void layoutDone() {
        if (parts.isEmpty()) {
            laidOut = true;
            return;
        }
        scrollPane.layout();
        laidOut = true;
        markNear();
        if (scrollPending) {
            scrollPending = false;
            scrollToCurrent();
        }
    }

    /**
     * Runs the new search text through the document.
     */
    private void searchChanged() {
        Found old = currentMatch();
        clearMarks();
        query = PhraseQuery.of(searchText.get());
        findAll();
        publish(old == null ? firstInDirection() : nearestInDirection(old));
        markNear();
        scrollToCurrent();
    }

    /**
     * Runs the search through the document again after a segment's texts changed their order or came and went: a
     * table the reader sorted, whose columns the reader moved, hid or showed. The matches are numbered anew. The
     * current match stays on its text if it is still there; else the first match after it in the old order that
     * still is becomes current, backward the first before it, wrapping around. The view does not scroll: the reader
     * changed the table, not the search.
     *
     * @param segment the segment whose texts changed
     */
    private void textsChanged(Searchable segment) {
        if (query.isEmpty()) {
            return;
        }
        int old = matchIndex.get();
        Found oldCurrent = currentMatch();
        // Named before the segment numbers its texts anew: it names them as it numbered them for these matches.
        List<LastingPlace> before = new ArrayList<>(matches.size());
        for (Found found : matches) {
            before.add(placeOf(found));
        }
        Searchable oldSegment = oldCurrent == null ? null : parts.get(oldCurrent.part()).segment();
        findAll();
        publish(oldCurrent == null ? firstInDirection() : firstStillThere(before, old));
        remarkSegment(segment);
        if (oldSegment != null) {
            remarkSegment(oldSegment);
        }
        Found current = currentMatch();
        if (current != null) {
            remarkSegment(parts.get(current.part()).segment());
        }
        markNear();
    }

    /**
     * The first of the matches before a change, from the current one on in the direction and wrapping around, that
     * is still there after it.
     *
     * @param before the places of the matches before the change, in their order then
     * @param old    the index of the current match among them
     * @return its index among the matches now; as for a new search if none is still there
     */
    private int firstStillThere(List<LastingPlace> before, int old) {
        Map<LastingPlace, Integer> now = new HashMap<>();
        for (int i = 0; i < matches.size(); i++) {
            now.put(placeOf(matches.get(i)), i);
        }
        int step = forward() ? 1 : -1;
        for (int k = 0; k < before.size(); k++) {
            Integer index = now.get(before.get(Math.floorMod(old + k * step, before.size())));
            if (index != null) {
                return index;
            }
        }
        return firstInDirection();
    }

    /**
     * Where a match lies, named so that it stays the same across a change of its segment's texts.
     */
    private LastingPlace placeOf(Found found) {
        Searchable segment = parts.get(found.part()).segment();
        return new LastingPlace(found.order(), segment.searchTextKey(found.match().text()), found.match().start());
    }

    /**
     * Has a segment that shows its marks show them anew: its matches, or the current match among them, changed. A
     * segment left without matches takes its marks away.
     */
    private void remarkSegment(Searchable segment) {
        if (!marked.contains(segment)) {
            return;
        }
        for (int part = 0; part < parts.size(); part++) {
            if (parts.get(part).segment() == segment) {
                show(part);
                return;
            }
        }
        segment.showSearchMatches(List.of(), -1);
        marked.remove(segment);
    }

    /**
     * The current match, {@code null} if there is none.
     */
    private Found currentMatch() {
        int current = matchIndex.get();
        return current < 0 || current >= matches.size() ? null : matches.get(current);
    }

    /**
     * Where a changed search goes on from the old current match: forward the first match that starts at or after
     * the old one's start, else the first; backward the last that starts at or before it, else the last. So the current
     * match stays while it still matches.
     *
     * @param old the current match of the search before
     * @return the index of the new current match, -1 without matches
     */
    private int nearestInDirection(Found old) {
        if (matches.isEmpty()) {
            return -1;
        }
        if (forward()) {
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i).compareStart(old) >= 0) {
                    return i;
                }
            }
            return 0;
        }
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i).compareStart(old) <= 0) {
                return i;
            }
        }
        return matches.size() - 1;
    }

    /**
     * Where a new search starts: forward at the first match, backward at the last; -1 without matches.
     */
    private int firstInDirection() {
        if (matches.isEmpty()) {
            return -1;
        }
        return forward() ? 0 : matches.size() - 1;
    }

    /**
     * Makes the next match in the direction the current one, wrapping around; nothing without matches.
     */
    void next() {
        step(forward() ? 1 : -1);
    }

    /**
     * Makes the next match against the direction the current one, wrapping around; nothing without matches.
     */
    void previous() {
        step(forward() ? -1 : 1);
    }

    /**
     * Moves the current match by one towards the end of the document (1) or its start (-1), wrapping around, and
     * shows the change in the marks of the segments of the old and the new current match.
     */
    private void step(int by) {
        if (matches.isEmpty()) {
            return;
        }
        int old = matchIndex.get();
        int current = Math.floorMod(old + by, matches.size());
        publish(current);
        remark(matches.get(old).part());
        remark(matches.get(current).part());
        markNear();
        scrollToCurrent();
    }

    /**
     * Whether the search goes forward: {@code null} counts as forward.
     */
    private boolean forward() {
        return direction.get() != SearchDirection.BACKWARD;
    }

    /**
     * Finds every match of the query in the document's searchable segments, in document order.
     */
    private void findAll() {
        matches.clear();
        parts.clear();
        if (query.isEmpty()) {
            return;
        }
        List<Segment> segments = selection.segments();
        for (int order = 0; order < segments.size(); order++) {
            if (!(segments.get(order) instanceof Searchable searchable)) {
                continue;
            }
            // A table tells when the reader reorders its texts; a later run replaces what it runs then.
            searchable.onSearchTextsChanged(() -> textsChanged(searchable));
            int first = matches.size();
            List<String> texts = searchable.searchTexts();
            for (int text = 0; text < texts.size(); text++) {
                for (Mark mark : query.find(texts.get(text))) {
                    matches.add(new Found(parts.size(), order, new Searchable.Match(text, mark.start(), mark.end())));
                }
            }
            if (matches.size() > first) {
                parts.add(new Part(searchable, first, matches.size() - first));
            }
        }
    }

    /**
     * Sets the view's number of matches and index of the current match. The two change in the order that keeps the
     * index below the number at every moment, so a listener of either never reads a current match beyond the last.
     *
     * @param current the index of the current match, -1 for none
     */
    private void publish(int current) {
        if (matches.size() >= matchCount.get()) {
            matchCount.set(matches.size());
            matchIndex.set(current);
        } else {
            matchIndex.set(current);
            matchCount.set(matches.size());
        }
    }

    // --- Scrolling ------------------------------------------------------------------------------------------------

    /**
     * Scrolls to the current match if it is not fully visible: its segment sideways first, if the segment scrolls
     * sideways itself, then the view so that the match's middle is at the middle of the visible area, as far as the
     * scroll range allows. A fully visible match scrolls nothing, so going through the matches on screen is calm.
     * Before the view has laid out the document it waits for that layout.
     *
     * <p>All of it in the pixels of the scroll pane's content, the zoom box, as the scroll offset is: so it holds at
     * every zoom.
     */
    private void scrollToCurrent() {
        Found current = currentMatch();
        if (current == null) {
            return;
        }
        double visibleHeight = scrollPane.getViewportBounds().getHeight();
        if (!laidOut || visibleHeight <= 0) {
            scrollPending = true;
            return;
        }
        Searchable segment = parts.get(current.part()).segment();
        segment.revealSideways(current.match());
        Bounds bounds = scrollPane.getContent().sceneToLocal(segment.searchMatchBounds(current.match()));
        double offset = VerticalScroll.offset(scrollPane);
        if (bounds.getMinY() >= offset && bounds.getMaxY() <= offset + visibleHeight) {
            return;
        }
        VerticalScroll.scrollTo(scrollPane, bounds.getCenterY() - visibleHeight / 2);
    }

    // --- Marks ----------------------------------------------------------------------------------------------------

    /**
     * Has every segment with matches near the visible area show its marks, unless it shows them already.
     */
    private void markNear() {
        if (!laidOut || parts.isEmpty()) {
            return;
        }
        NearArea area = NearArea.of(scrollPane);
        if (area == null) {
            return;
        }
        // The segments with matches lie one below the other in document order.
        int first = area.firstReaching(parts.size(), part -> parts.get(part).segment().node());
        for (int i = first; i < parts.size(); i++) {
            Searchable segment = parts.get(i).segment();
            if (area.boundsOf(segment.node()).getMinY() > area.bottom()) {
                break;
            }
            if (!marked.contains(segment)) {
                show(i);
            }
            // A long table marks the rows near the area only, more as the view scrolls.
            segment.showSearchMatchesNear(area);
        }
    }

    /**
     * Has a segment with matches show them anew, if it shows its marks: the current match has come or gone.
     */
    private void remark(int part) {
        if (marked.contains(parts.get(part).segment())) {
            show(part);
        }
    }

    /**
     * Has a segment with matches show them, the current one among them if it lies there.
     */
    private void show(int part) {
        Part shown = parts.get(part);
        List<Searchable.Match> shownMatches = new ArrayList<>(shown.count());
        for (int i = shown.first(); i < shown.first() + shown.count(); i++) {
            shownMatches.add(matches.get(i).match());
        }
        int current = matchIndex.get() - shown.first();
        shown.segment().showSearchMatches(shownMatches, current >= 0 && current < shown.count() ? current : -1);
        marked.add(shown.segment());
    }

    /**
     * Takes the marks away from every segment that shows them.
     */
    private void clearMarks() {
        for (Searchable segment : marked) {
            segment.showSearchMatches(List.of(), -1);
        }
        marked.clear();
    }

    /**
     * Where a current match lies, for a new version of the text.
     *
     * @param segment its segment, noted to be found again
     * @param textKey what names its text in the segment across a new order of the texts
     *                ({@link Searchable#searchTextKey(int)})
     * @param match   where it lies in the segment
     */
    record Place(NotedSegment segment, Object textKey, Searchable.Match match) {
    }

    /**
     * A segment with matches.
     *
     * @param segment the segment
     * @param first   the index of its first match among all matches
     * @param count   the number of its matches, at least one
     */
    private record Part(Searchable segment, int first, int count) {
    }

    /**
     * Where a match lies in the shown document, the same across a change of its segment's texts' order
     * ({@link Searchable#searchTextKey}).
     *
     * @param order the segment's place in document order, among all segments
     * @param text  what names the match's text in the segment
     * @param start the match's first character in the text
     */
    private record LastingPlace(int order, Object text, int start) {
    }

    /**
     * A match of the document.
     *
     * @param part  the index of its segment among the segments with matches
     * @param order the segment's place in document order, among all segments
     * @param match where it lies in the segment
     */
    private record Found(int part, int order, Searchable.Match match) {

        /**
         * Compares where two matches of the same document start, in document order.
         *
         * @return negative if this one starts before the other, 0 at the same place, positive after it
         */
        int compareStart(Found other) {
            if (order != other.order) {
                return Integer.compare(order, other.order);
            }
            if (match.text() != other.match.text()) {
                return Integer.compare(match.text(), other.match.text());
            }
            return Integer.compare(match.start(), other.match.start());
        }
    }
}
