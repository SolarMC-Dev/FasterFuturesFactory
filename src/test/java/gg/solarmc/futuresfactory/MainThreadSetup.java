package gg.solarmc.futuresfactory;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainThreadSetup implements BeforeEachCallback, ParameterResolver {

	private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(getClass());

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		extensionContext.getStore(namespace).put(ClosableScheduledExecutor.class,
				new ClosableScheduledExecutor(Executors.newSingleThreadScheduledExecutor()));
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return parameterContext.getParameter().getType().equals(ScheduledExecutorService.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return extensionContext.getStore(namespace)
				.get(ClosableScheduledExecutor.class, ClosableScheduledExecutor.class).scheduledExecutor();
	}
}
