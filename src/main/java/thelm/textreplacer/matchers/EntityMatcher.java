package thelm.textreplacer.matchers;

import java.util.function.Predicate;

import net.minecraft.entity.Entity;

public class EntityMatcher implements Predicate<Entity> {

	@Override
	public boolean test(Entity entity) {
		return true;
	}
}
