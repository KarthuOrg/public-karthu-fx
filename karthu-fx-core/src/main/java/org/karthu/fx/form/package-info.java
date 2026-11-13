/**
 * Form inputs that look and behave alike.
 *
 * <p>Every input implements one contract, {@link org.karthu.fx.form.FormInput}: its node, its value, its
 * {@linkplain org.karthu.fx.form.FieldBinding binding} to a variable, read-only, its
 * {@linkplain org.karthu.fx.form.FieldState state} and the messages it reports itself. Every input sits in the same
 * shell, {@link org.karthu.fx.form.FieldShell}, one border around its content, or in the shell's single-line form,
 * {@link org.karthu.fx.form.FieldFrame}.
 *
 * <p>The inputs: {@link org.karthu.fx.form.TextInput}, {@link org.karthu.fx.form.PasswordInput},
 * {@link org.karthu.fx.form.NumberInput}, {@link org.karthu.fx.form.ComboInput},
 * {@link org.karthu.fx.form.EditableComboInput}, {@link org.karthu.fx.form.TextAreaInput},
 * {@link org.karthu.fx.form.PathInput} and {@link org.karthu.fx.form.CronInput}. A
 * {@link org.karthu.fx.form.FormField} is the row of a form: the label, the input and the
 * {@linkplain org.karthu.fx.form.FieldMessage messages} under it. In the same shell there are a search field,
 * {@link org.karthu.fx.form.SearchInput}, and one with a popup of suggestions,
 * {@link org.karthu.fx.form.SuggestionInput}.
 *
 * <p>The style classes and pseudo-classes each type documents are a public contract. Like all JavaFX controls, the
 * inputs are used on the JavaFX Application Thread only.
 */
package org.karthu.fx.form;
