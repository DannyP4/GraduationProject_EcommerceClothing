package com.uniform.store.service;

import java.util.List;

public interface TranslationProvider {

    boolean isEnabled();

    /**
     * Translate a list of texts into the target locale. 
     * The provider may choose to return fewer translations than the input list, but must not return more.
     */
    List<String> translate(List<String> texts, String targetLocale);
}
