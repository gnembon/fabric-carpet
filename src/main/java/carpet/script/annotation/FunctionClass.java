package carpet.script.annotation;

/**
 * Represents a class that provides functions to the Scarpet language.
 * 
 * @see LazyFunction
 *
 */
public interface FunctionClass {
	/**
	 * @return The name (id) of the provider of functions in this class
	 */
	String getProvider();
}
