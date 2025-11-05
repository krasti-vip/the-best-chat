package ru.test.the.best.chat.core.service;

import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.errs.UnitResult;

import java.util.List;
import java.util.Optional;

public interface Service<T, I, D> {

    List<T> findAll() throws Exception;

    Result<Optional<T>, Error> findById(final I id) throws Exception;

    UnitResult<Error> deleteById(final I id);

    UnitResult<Error> update(final I id, final D object);

    UnitResult<Error> save(final D object);
}
