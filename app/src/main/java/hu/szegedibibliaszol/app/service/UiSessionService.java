package hu.szegedibibliaszol.app.service;

import hu.szegedibibliaszol.app.ui.model.AppSessionSnapshot;
import java.util.Optional;

public interface UiSessionService {

    Optional<AppSessionSnapshot> loadSession();

    void saveSession(AppSessionSnapshot sessionSnapshot);
}

