package mytown.protection.segment;

import com.google.gson.*;
import myessentials.utils.StringUtils;
import mytown.api.container.GettersContainer;
import mytown.entities.flag.FlagType;
import mytown.util.exceptions.ConditionException;

import java.lang.reflect.Type;

/**
 * A part of the protection that protects against a specific thing.
 */
public class Segment implements JsonSerializer<Segment>, JsonDeserializer<Segment> {
    protected final Class<?> theClass;
    protected final FlagType flag;
    protected final Object denialValue;
    protected final String[] conditionString;

    public final GettersContainer getters = new GettersContainer();

    public Segment(Class<?> theClass, GettersContainer getters, FlagType flag, Object denialValue, String conditionString) {
        this.theClass = theClass;
        this.flag = flag;
        this.denialValue = denialValue;

        if(conditionString != null) {
            this.conditionString = conditionString.split(" ");
        } else {
            this.conditionString = null;
        }

        if(getters != null) {
            this.getters.addAll(getters);
        }
    }

    public Class<?> getCheckClass() {
        return theClass;
    }

    public FlagType getFlag() {
        return flag;
    }

    public Object getDenialValue() {
        return denialValue;
    }

    public String[] getConditionString() {
        return conditionString;
    }

    public boolean checkCondition(Object object) {

        if(conditionString == null)
            return true;

        //MyTown.instance.log.info("Checking condition: " + StringUtils.join(conditionString, " "));
        boolean current;

        Object instance;
        instance = object;

        for(int i = 0; i < conditionString.length; i += 4) {

            // Get the boolean value of each part of the condition.
            if(StringUtils.tryParseBoolean(conditionString[i + 2])) {
                boolean value = (Boolean) getters.get(conditionString[i]).invoke(Boolean.class, instance, object);
                if ("==".equals(conditionString[i + 1])) {
                    current = value == Boolean.parseBoolean(conditionString[i + 2]);
                } else if("!=".equals(conditionString[i + 1])) {
                    current = value != Boolean.parseBoolean(conditionString[i + 2]);
                } else {
                    throw new ConditionException("[Segment: " + this.theClass.getName() + "] The element number " + (i / 4) + 1 + " has an invalid condition!");
                }
            } else if(StringUtils.tryParseInt(conditionString[i + 2])) {
                int value = (Integer) getters.get(conditionString[i]).invoke(Integer.class, instance, object);
                if("==".equals(conditionString[i + 1])) {
                    current = value == Integer.parseInt(conditionString[i + 2]);
                } else if("!=".equals(conditionString[i + 1])) {
                    current = value != Integer.parseInt(conditionString[i + 2]);
                } else if("<".equals(conditionString[i + 1])) {
                    current = value < Integer.parseInt(conditionString[i + 2]);
                } else if(">".equals(conditionString[i + 1])) {
                    current = value > Integer.parseInt(conditionString[i + 2]);
                } else {
                    throw new ConditionException("[Segment: "+ this.theClass.getName() +"] The element number " + (i / 4) + 1 + " has an invalid condition!");
                }
            } else if(StringUtils.tryParseFloat(conditionString[i + 2])) {
                float value = (Integer) getters.get(conditionString[i]).invoke(Integer.class, instance, object);
                if("==".equals(conditionString[i + 1])) {
                    current = value == Float.parseFloat(conditionString[i + 2]);
                } else if("!=".equals(conditionString[i + 1])) {
                    current = value != Float.parseFloat(conditionString[i + 2]);
                } else if("<".equals(conditionString[i + 1])) {
                    current = value < Float.parseFloat(conditionString[i + 2]);
                } else if(">".equals(conditionString[i + 1])) {
                    current = value > Float.parseFloat(conditionString[i + 2]);
                } else {
                    throw new ConditionException("[Segment: "+ this.theClass.getName() +"] The element number " + ((i/4)+1) + " has an invalid condition!");
                }
            } else if(conditionString[i + 2].startsWith("'") && conditionString[i+2].endsWith("'")){
                String value = (String) getters.get(conditionString[i]).invoke(String.class, instance, object);
                if("==".equals(conditionString[i + 1])) {
                    current = value.equals(conditionString[i+2].substring(1, conditionString[i+2].length() - 1));
                } else if("!=".equals(conditionString[i + 1])) {
                    current = !value.equals(conditionString[i+2].substring(1, conditionString[i+2].length() - 1));
                } else {
                    throw new ConditionException("[Segment: "+ this.theClass.getName() +"] The element number " + ((i/4)+1) + " has an invalid condition!");
                }
            } else {
                throw new ConditionException("[Segment: "+ this.theClass.getName() +"] The element with number " + ((i/4)+1) + " has an invalid type to be checked against!");
            }

            if(conditionString.length <= i + 3 || current && "OR".equals(conditionString[i + 3]) || !current && "AND".equals(conditionString[i + 3]))
                return current;

            if(!"OR".equals(conditionString[i + 3]) && !"AND".equals(conditionString[i + 3]))
                throw new ConditionException("[Segment: "+ this.theClass.getName()  +"] Invalid condition element: " + conditionString[i + 3]);
        }
        return false;
    }

    /**
     * Gets the range of the area of effect of this thing, or 0 if none is specified.
     */
    public int getRange(Object object) {
        return getters.contains("range") ? (Integer) getters.get("range").invoke(Integer.class, object, object) : 0;
    }

    @Override
    public JsonElement serialize(Segment src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }

    @Override
    public Segment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return null;
    }
}
