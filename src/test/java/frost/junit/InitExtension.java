package frost.junit;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import frost.AppHome;

public class InitExtension implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
		AppHome.init();
	}
}