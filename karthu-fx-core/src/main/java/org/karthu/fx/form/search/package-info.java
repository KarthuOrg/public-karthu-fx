/**
 * The matching rules of search, independent of JavaFX, which share one folding of case and accents: the search and
 * suggestion inputs' rule, which entries match a search and where their marks go, and which the shortcut editor's
 * search shares, so that it ignores case and accents exactly as the search inputs do; and the Markdown view's rule,
 * where a search as one piece matches in a text. Internal: the package is not exported, so the rules can change without
 * an API change.
 */
package org.karthu.fx.form.search;
