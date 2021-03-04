# HandleWrapper

[MethodHandles](https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/MethodHandle.html) can be as fast as 
direct invocation, as long as they are static final fields. Then they can be 
inlined. But what if we want to use them dynamically? There's definitely simpler 
solutions than this one, but I wanted to learn a thing or 2 about bytecode, so here I use [ASM](https://asm.ow2.io/) to create classes at runtime that contain the MethodHandle as a static final field. These "HandleWrappers" are almost as fast as direct invocation.

```text
Benchmark                                        Mode  Cnt  Score   Error  Units
QuickBenchmark.benchmarkDirect                   avgt   10  3,601 ± 0,015  ns/op
QuickBenchmark.benchmarkStaticFinalMethodHandle  avgt   10  3,619 ± 0,027  ns/op
QuickBenchmark.benchmarkWrapper                  avgt   10  3,839 ± 0,043  ns/op
```

## Usage
Assume we want to call following method:

```java
public class SomeClass {
    private int someMethod(Object someArg, String someOtherArg) { ... }
}
```
First we need to obtain a MethodHandle for the method:

```java
Method method = SomeClass.class.getDeclaredMethod("someMethod", Object.class, String.class);
method.setAccessible(true);
MethodHandle handle = MethodHandles.lookup().unreflect(method);
```
Now we can use the WrapperFactory to wrap the Handle and use it:

```java
HandleWrapper wrapper = WrapperFactory.wrap(handle, SomeClass.class, false, int.class, Object.class, String.class);
int result = (int) wrapper.invoke(objectOfSomeClass, someArg, "someOtherArg");
```
You can also wrap your MethodHandle in an interface, the interfaces methods 
can target all public methods of the MethodHandle class, they just need to have the same signature, and if they target a SignaturePolymorphic method like invoke or
invokeExact they need to have the same Signature as the target method (If the method is not static the first argument needs to be an Object of the targeted class).
```java
public interface CustomInterface {
    int accessSomeMethod(SomeClass target, Object someArg, String someOtherArg);
}
```
Now declare which methods from the interface link to which MethodHandle method and call the WrapperFactory.
```java
Method from = CustomInterface.class.getDeclaredMethod(accessSomeMethod ...);
Method to   = MethodHandle.class.getDeclaredMethod("invoke", Object[].class);
Map<Method, Method> links = new HashMap<>();
links.put(from, to);

CustomInterface customWrapper = WrapperFactory.wrap(CustomInterface.class, links, handle);
int result = customWrapper.accessSomeMethod(target, someArg, "someOtherArg");
```

## License
The contents of this project are licensed under the [MIT license](LICENSE).



