package kilanny.autocaller.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Created by user on 11/7/2017.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface PerActivityOrService {
}
