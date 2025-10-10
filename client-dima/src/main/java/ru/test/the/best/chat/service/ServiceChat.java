package ru.test.the.best.chat.service;

import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.errs.UnitResult;

import java.util.List;
import java.util.Optional;

public interface ServiceChat<T, I, D> {

    List<T> findAll();

    Result<Optional<T>, Error> findById(final I id);

    UnitResult<Error> deleteById(final I id);

    UnitResult<Error> updateById(final I id, final D object);

    UnitResult<Error> save(final D object);
}
