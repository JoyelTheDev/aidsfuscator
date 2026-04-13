package dev.lvstrng.aidsfuscator.transform.impl.data;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Modifier;

/**
 * Moves constants (final, static fields) into <clinit>.
 *
 * @author jonesdevelopment
 */
public class ConstantsFixTransformer extends Transformer {
  public ConstantsFixTransformer() {
    super("Fix Constants", "fixConstants");
  }

  @Override
  public void transform(Context context) {
    for (JClass clazz : context.classes()) {
      if (Exclusions.FIX_CONSTANTS.excluded(clazz)) {
        continue;
      }

      for (JField field : clazz.fields()) {
        if (field.value() == null) {
          continue;
        }

        if (Exclusions.FIX_CONSTANTS.excluded(field)) {
          continue;
        }

        // First, move the initialization to `<clinit>`. Then, delete the value.
        InsnBuilder builder = new InsnBuilder()
            ._const(field.value())
            .field(PUTSTATIC, clazz.name(), field.name(), field.desc());
        clazz.findOrCreateClinit().insertSafe(builder.result());
        field.setValue(null);

        markChange();
      }
    }
  }
}
