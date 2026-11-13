package org.karthu.fx.markdown;

import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;

/**
 * What an image shows when it cannot be shown, and the limit above which it cannot: the view's notes of a missing
 * image, of one in a format the view cannot decode and of one above the pixel limit, and the pixel limit itself. The
 * images of the document ({@link DocumentImages}) and the full size window ({@link ImageWindowContent}) share them,
 * each reading the view's properties at the time it uses them: a box binds its note, a decode reads the limit when it
 * is queued.
 *
 * @param missing     the note of an image the loader has no file for, the view's text property
 * @param unsupported the note of an image JavaFX cannot decode, the view's text property
 * @param tooLarge    the note of an image above the pixel limit, the view's text property
 * @param pixelLimit  the largest image in pixels the view shows, the view's property, 0 or less for no limit
 */
record ImageNotes(ObservableValue<String> missing, ObservableValue<String> unsupported,
                  ObservableValue<String> tooLarge, ObservableIntegerValue pixelLimit) {
}
