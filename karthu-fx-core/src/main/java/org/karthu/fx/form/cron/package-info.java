/**
 * The rules of the cron input, independent of JavaFX: they check a Unix or Quartz cron expression, parse it,
 * describe it in plain English and convert it into the other format. Internal: the package is not exported, so the
 * parsed form, the messages and the plain text can change without an API change.
 */
package org.karthu.fx.form.cron;
