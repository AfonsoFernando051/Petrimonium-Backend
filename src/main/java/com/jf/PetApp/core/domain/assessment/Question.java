package com.jf.PetApp.core.domain.assessment;

import java.util.List;

public record Question(
    String id,
    String text,
    List<Option> options
) {}
