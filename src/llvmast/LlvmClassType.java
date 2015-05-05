package llvmast;

public class LlvmClassType extends LlvmType {
	public String name;
	
	public LlvmClassType(String name) {
		this.name = name;
	}

	public String toString() {
		return "%class."+name;
	}
}
