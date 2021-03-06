package com.zhsan.gameobject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.zhsan.common.GlobalVariables;
import com.zhsan.common.Point;
import com.zhsan.common.Utility;
import com.zhsan.common.exception.FileReadException;
import com.zhsan.common.exception.FileWriteException;
import com.zhsan.gamecomponents.GlobalStrings;
import com.zhsan.gamecomponents.common.XmlHelper;
import com.zhsan.lua.LuaAI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;

/**
 * Created by Peter on 24/5/2015.
 */
public class Architecture implements HasPointBelongsFactionGameObject {

    public static final String SAVE_FILE = "Architecture.csv";

    private GameScenario scenario;

    private String name;
    private String nameImageName;

    private List<Point> location;

    private ArchitectureKind architectureKind;
    private Section belongedSection;

    private int population;
    private int fund, food;
    private float agriculture, commerce, technology, endurance, morale;

    private GameObjectList<MilitaryKind> creatableMilitaryKinds;

    private Set<Integer> connectedArchitectures = new HashSet<>();

    private final int id;
    private String aiTags = "";

    @Override
    @LuaAI.ExportToLua
    public int getId() {
        return id;
    }

    @Override
    @LuaAI.ExportToLua
    public String getAiTags() {
        return aiTags;
    }

    @Override
    @LuaAI.ExportToLua
    public GameObject setAiTags(String aiTags) {
        this.aiTags = aiTags;
        return this;
    }

    private Architecture(int id, GameScenario scen) {
        this.id = id;
        this.scenario = scen;
    }

    public static final GameObjectList<Architecture> fromCSV(FileHandle root, @NotNull GameScenario scen) {
        GameObjectList<Architecture> result = new GameObjectList<>();

        FileHandle f = root.child(Architecture.SAVE_FILE);
        try (CSVReader reader = new CSVReader(new InputStreamReader(f.read(), "UTF-8"))) {
            String[] line;
            int index = 0;
            while ((line = reader.readNext()) != null) {
                index++;
                if (index == 1) continue; // skip first line.

                Architecture data = new Architecture(Integer.parseInt(line[0]), scen);
                data.setAiTags(line[1]);
                data.nameImageName = line[2];
                data.name = line[3];
                data.architectureKind = scen.getArchitectureKinds().get(Integer.parseInt(line[4]));
                data.location = Point.fromCSVList(line[5]);
                data.belongedSection = scen.getSections().get(Integer.parseInt(line[6]));
                data.population = Integer.parseInt(line[7]);
                data.fund = Integer.parseInt(line[8]);
                data.food = Integer.parseInt(line[9]);
                data.agriculture = Float.parseFloat(line[10]);
                data.commerce = Float.parseFloat(line[11]);
                data.technology = Float.parseFloat(line[12]);
                data.morale = Float.parseFloat(line[13]);
                data.endurance = Float.parseFloat(line[14]);
                data.creatableMilitaryKinds = scen.getMilitaryKinds().getItemsFromCSV(line[15]);
                data.connectedArchitectures = new HashSet<>(XmlHelper.loadIntegerListFromXml(line[16]));

                result.add(data);
            }
        } catch (IOException e) {
            throw new FileReadException(f.path(), e);
        }

        return result;
    }

    public static final void toCSV(FileHandle root, GameObjectList<Architecture> data) {
        FileHandle f = root.child(SAVE_FILE);
        try (CSVWriter writer = new CSVWriter(f.writer(false, "UTF-8"))) {
            writer.writeNext(GlobalStrings.getString(GlobalStrings.Keys.ARCHITECTURE_SAVE_HEADER).split(","));
            for (Architecture d : data) {
                writer.writeNext(new String[]{
                        String.valueOf(d.getId()),
                        d.getAiTags(),
                        d.nameImageName,
                        d.getName(),
                        String.valueOf(d.architectureKind.getId()),
                        Point.toCSVList(d.location),
                        String.valueOf(d.belongedSection == null ? -1 : d.belongedSection.getId()),
                        String.valueOf(d.population),
                        String.valueOf(d.fund),
                        String.valueOf(d.food),
                        String.valueOf(d.agriculture),
                        String.valueOf(d.commerce),
                        String.valueOf(d.technology),
                        String.valueOf(d.endurance),
                        String.valueOf(d.morale),
                        d.creatableMilitaryKinds.toCSV(),
                        XmlHelper.saveIntegerListToXml(d.connectedArchitectures)
                });
            }
        } catch (IOException e) {
            throw new FileWriteException(f.path(), e);
        }

    }

    @Override
    @LuaAI.ExportToLua
    public String getName() {
        return name;
    }

    public String getNameImageName() {
        return nameImageName;
    }

    public List<Point> getLocations() {
        return location;
    }

    @LuaAI.ExportToLua
    public ArchitectureKind getKind() {
        return architectureKind;
    }

    @LuaAI.ExportToLua
    public Section getBelongedSection() {
        return belongedSection;
    }

    @LuaAI.ExportToLua
    public Faction getBelongedFaction() {
        return belongedSection == null ? null : belongedSection.getBelongedFaction();
    }

    private void changeFaction(Faction newFaction) {
        if (this.getBelongedFaction() != null) {
            Architecture moveTo = this.getBelongedFaction().getArchitectures().getAll()
                    .stream()
                    .filter(x -> x != this)
                    .min((x, y) -> Double.compare(this.getPosition().distanceTo(x.getPosition()), this.getPosition().distanceTo(y.getPosition())))
                    .orElse(null);
            if (moveTo != null) {
                this.getPersons().forEach(p -> p.moveToArchitecture(moveTo));
            }
        }
    }

    public void changeSection(Section newSection) {
        if (this.belongedSection != null && this.belongedSection.getBelongedFaction() != newSection.getBelongedFaction()) {
            changeFaction(newSection.getBelongedFaction());
        }
        this.belongedSection = newSection;
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getPersonsIncludingMoving() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.NORMAL);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getPersons() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.NORMAL && p.getMovingDays() == 0);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getUnhiredPersons() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.UNEMPLOYED);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getPersonsExcludingMayor() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.NORMAL &&
                p.getDoingWorkType() != Person.DoingWork.MAYOR);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getPersonsWithoutLeadingMilitary() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.NORMAL &&
                getMilitaries().filter(m -> m.getLeader() == p).size() == 0);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Person> getPersonsNotInMilitary() {
        return scenario.getPersons().filter(p -> p.getLocation() == this && p.getState() == Person.State.NORMAL &&
                getMilitaries().filter(m -> m.getAllPersons().contains(p)).size() == 0);
    }

    public boolean hasFaction() {
        return getBelongedFaction() != null;
    }

    public GameObjectList<Facility> getFacilities() {
        return scenario.getFacilities().filter(f -> f.getBelongedArchitecture() == this);
    }

    public String getFactionName() {
        return this.getBelongedFaction() == null ? GlobalStrings.getString(GlobalStrings.Keys.NO_CONTENT) :
                this.getBelongedFaction().getName();
    }

    public String getArchitectureKindName() {
        return this.getKind().getName();
    }

    @LuaAI.ExportToLua
    public int getPopulation() {
        return population;
    }

    @LuaAI.ExportToLua
    public int getFund() {
        return fund;
    }

    @LuaAI.ExportToLua
    public int getFood() {
        return food;
    }

    public String getFoodString() {
        return food / Integer.parseInt(GlobalStrings.getString(GlobalStrings.Keys.FOOD_UNIT)) +
                GlobalStrings.getString(GlobalStrings.Keys.FOOD_UNIT_STRING);
    }

    @LuaAI.ExportToLua
    public float getAgriculture() {
        return agriculture;
    }

    @LuaAI.ExportToLua
    public float getCommerce() {
        return commerce;
    }

    @LuaAI.ExportToLua
    public float getTechnology() {
        return technology;
    }

    @LuaAI.ExportToLua
    public float getEndurance() {
        return endurance;
    }

    @LuaAI.ExportToLua
    public float getMorale() {
        return morale;
    }

    Person pickMayor(Person exclude) {
        if (this.getBelongedFaction() == null) {
            return null;
        }

        Person leader = this.getBelongedFaction().getLeader();
        if (this.getPersons().contains(leader) && leader != exclude) {
            return this.getBelongedFaction().getLeader();
        }

        return this.getPersons().filter(p -> p != exclude).max((p, q) -> Integer.compare(p.getAbilitySum(), q.getAbilitySum()), null);
    }

    GameObjectList<Person> getMayorUnchecked() {
        return this.getPersons().filter(person -> person.getDoingWorkType() == Person.DoingWork.MAYOR);
    }

    public Person getMayor() {
        if (this.getPersons().size() == 0) return null;
        GameObjectList<Person> p = this.getPersons().filter(person -> person.getDoingWorkType() == Person.DoingWork.MAYOR);
        if (p.size() != 1) {
            throw new IllegalStateException("There should be one and only one mayor in every architecture");
        }
        return p.getFirst();
    }

    @LuaAI.ExportToLua
    public boolean canChangeMayorToOther() {
        return this.getPersons().size() > 0 && this.getBelongedFaction() != null && this.getBelongedFaction().getLeader().getLocation() != this;
    }

    @LuaAI.ExportToLua
    public void changeMayor(int id) {
        changeMayor(scenario.getPerson(id), false);
    }

    public void changeMayor(Person newMayor, boolean leaderLeaving) {
        if (newMayor.getLocation() != this) {
            throw new IllegalStateException("The new mayor must be in the architecture");
        }
        if (this.getBelongedFaction() == null) {
            throw new IllegalStateException("Empty architectures may not have mayors");
        }
        if (this.getBelongedFaction().getLeader().getLocation() == this && !leaderLeaving) {
            throw new IllegalStateException("May not set mayor if the leader is in architecture");
        }
        newMayor.setDoingWork(Person.DoingWork.MAYOR);
    }

    public boolean hasMayor() {
        return getMayorUnchecked().size() > 0;
    }

    public void addMayor() {
        Person newMayor = pickMayor(null);
        if (newMayor == null) return;

        if (getMayorUnchecked().size() > 0) {
            throw new IllegalStateException("addMayor may only be used when there is no mayor at all");
        }
        if (newMayor.getLocation() != this) {
            throw new IllegalStateException("The new mayor must be in the architecture");
        }
        newMayor.setDoingWorkUnchecked(Person.DoingWork.MAYOR);
    }

    public GameObjectList<Person> getWorkingPersons(Predicate<Person.DoingWork> p) {
        return this.getPersons().filter(person -> p.test(person.getDoingWorkType()));
    }

    public GameObjectList<Person> getWorkingPersons(Person.DoingWork doingWork) {
        return getWorkingPersons(x -> x == doingWork);
    }

    public GameObjectList<Person> getAgriculturePersons() {
        return getWorkingPersons(Person.DoingWork.AGRICULTURE);
    }

    public GameObjectList<Person> getCommercePersons() {
        return getWorkingPersons(Person.DoingWork.COMMERCE);
    }

    public GameObjectList<Person> getTechnologyPersons() {
        return getWorkingPersons(Person.DoingWork.TECHNOLOGY);
    }

    public GameObjectList<Person> getEndurancePersons() {
        return getWorkingPersons(Person.DoingWork.ENDURANCE);
    }

    public GameObjectList<Person> getMoralePersons() {
        return getWorkingPersons(Person.DoingWork.MORALE);
    }

    @LuaAI.ExportToLua
    public int getMilitaryCount() {
        return this.getMilitaries().size();
    }

    @LuaAI.ExportToLua
    public double getMilitaryUnitCount() {
        return this.getMilitaries().getAll().stream().mapToDouble(Military::getUnitCount).sum();
    }

    @LuaAI.ExportToLua
    public double getMilitaryUnitCountInFullRecruit() {
        return this.getMilitaries().getAll().stream().mapToDouble(m -> m.getKind().getMaxUnitCount()).sum();
    }

    @LuaAI.ExportToLua
    public GameObjectList<Military> getMilitaries() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this);
    }

    public GameObjectList<Military> getMilitariesWithLeader() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this && x.getLeader() != null);
    }

    public GameObjectList<Military> getMilitariesWithoutLeader() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this && x.getLeader() == null);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Military> getRecruitableMilitaries() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this && x.recruitable());
    }

    @LuaAI.ExportToLua
    public GameObjectList<Military> getSelectTrainableMilitaries() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this && (x.getQuantity() > 0 || this.getRecruitableMilitaries().size() > 0) &&
                (x.getMorale() < GlobalVariables.maxMorale || x.getCombativity() < GlobalVariables.maxCombativity));
    }

    @LuaAI.ExportToLua
    public GameObjectList<Military> getTrainableMilitaries() {
        return scenario.getMilitaries().filter(x -> x.getLocation() == this && x.trainable());
    }

    public GameObjectList<MilitaryKind> getCreatableMilitaryKinds() {
        return creatableMilitaryKinds;
    }

    @LuaAI.ExportToLua
    public GameObjectList<MilitaryKind> getActualCreatableMilitaryKinds() {
        GameObjectList<MilitaryKind> kinds = new GameObjectList<>();
        for (MilitaryKind k : scenario.getMilitaryKinds()) {
            if (!k.isCanOnlyCreateAtArchitecture() || creatableMilitaryKinds.contains(k)) {
                int cost = k.getCost(this);
                if (cost <= this.fund) {
                    kinds.add(k);
                }
            }
        }
        return kinds;
    }

    @LuaAI.ExportToLua
    public List<Point> getCampaignablePositions() {
        Set<Point> result = new HashSet<>();
        for (Point p : this.getLocations()) {
            Iterator<Point> it = p.spiralOutIterator(1);
            while (it.hasNext()) {
                Point q = it.next();
                // TODO consider military kind
                if (scenario.getTroopAt(q) == null && scenario.getTerrainAt(q).isPassableByAnyMilitaryKind(scenario)) {
                    result.add(q);
                }
            }
        }
        return new ArrayList<>(result);
    }

    @LuaAI.ExportToLua
    public double distanceTo(int architectureId) {
        return distanceTo(scenario.getArchitectures().get(architectureId));
    }

    public double distanceTo(Architecture a) {
        return getPosition().distanceTo(a.getPosition());
    }

    public Point getPosition() {
        return Point.getCentroid(location);
    }

    @LuaAI.ExportToLua
    public Military createMilitary(int kindId) {
        return createMilitary(scenario.getMilitaryKinds().get(kindId));
    }

    public Military createMilitary(MilitaryKind kind) {
        return scenario.createMilitary(this, kind);
    }

    public void loseFund(int x) {
        fund = Math.max(fund - x, 0);
    }

    public void losePopulation(int x) {
        population = Math.max(population - x, 0);
    }

    public boolean loseEndurance(int quantity) {
        this.endurance = Math.max(0, this.endurance - quantity);
        return this.endurance <= 0;
    }

    public void advanceDay() {
        loseInternal();
        developInternal();
        recruitMilitaries();
        trainMilitaries();
        if (scenario.getGameDate().getDayOfMonth() == 1) {
            gainResources();
        }
    }

    private void loseInternal() {
        this.agriculture = MathUtils.clamp(this.agriculture - GlobalVariables.internalDrop, 0, Float.MAX_VALUE);
        this.commerce = MathUtils.clamp(this.commerce - GlobalVariables.internalDrop, 0, Float.MAX_VALUE);
        this.technology = MathUtils.clamp(this.technology - GlobalVariables.internalDrop, 0, Float.MAX_VALUE);
        this.endurance = MathUtils.clamp(this.endurance - GlobalVariables.internalDrop, 0, Float.MAX_VALUE);
        this.morale = MathUtils.clamp(this.morale - GlobalVariables.internalDrop, 0, Float.MAX_VALUE);
    }

    private void developInternal() {
        Person mayor = this.getMayor();

        GameObjectList<Person> agricultureWorkingPersons = getWorkingPersons(Person.DoingWork.AGRICULTURE);
        GameObjectList<Person> commerceWorkingPersons = getWorkingPersons(Person.DoingWork.COMMERCE);
        GameObjectList<Person> technologyWorkingPersons = getWorkingPersons(Person.DoingWork.TECHNOLOGY);
        GameObjectList<Person> moraleWorkingPersons = getWorkingPersons(Person.DoingWork.MORALE);
        GameObjectList<Person> enduranceWorkingPersons = getWorkingPersons(Person.DoingWork.ENDURANCE);

        int totalWorkingPersons = (agricultureWorkingPersons.size() + commerceWorkingPersons.size() + technologyWorkingPersons.size() +
                moraleWorkingPersons.size() + enduranceWorkingPersons.size() + 1); // for extra mayor
        int totalCost = GlobalVariables.internalCost * totalWorkingPersons;

        if (totalCost > fund) {
            int affordable = fund / GlobalVariables.internalCost;
            if (affordable < 1) {
                return; // skip development entirely - mayor can't do work.
            }
            getWorkingPersons(x -> x != Person.DoingWork.NONE && x != Person.DoingWork.MAYOR)
                    .shuffledList().subList(0, totalWorkingPersons - affordable) // mayor must be working
                    .forEach(p -> p.setDoingWork(Person.DoingWork.NONE));

            agricultureWorkingPersons = getWorkingPersons(Person.DoingWork.AGRICULTURE);
            commerceWorkingPersons = getWorkingPersons(Person.DoingWork.COMMERCE);
            technologyWorkingPersons = getWorkingPersons(Person.DoingWork.TECHNOLOGY);
            moraleWorkingPersons = getWorkingPersons(Person.DoingWork.MORALE);
            enduranceWorkingPersons = getWorkingPersons(Person.DoingWork.ENDURANCE);

            totalCost = GlobalVariables.internalCost * affordable;
        }

        loseFund(totalCost);

        float agricultureAbility = (mayor == null ? 0 : mayor.getAgricultureAbility()) * GlobalVariables.mayorInternalWorkEfficiency +
                agricultureWorkingPersons.getAll().stream()
                        .map(p -> (float) p.getAgricultureAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor));
        this.agriculture = Utility.diminishingGrowth(
                this.agriculture, agricultureAbility * GlobalVariables.internalGrowthFactor, this.getKind().getAgriculture());

        float commerceAbility = (mayor == null ? 0 : mayor.getCommerceAbility()) * GlobalVariables.mayorInternalWorkEfficiency +
                commerceWorkingPersons.getAll().stream()
                        .map(p -> (float) p.getCommerceAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor));
        this.commerce = Utility.diminishingGrowth(
                this.commerce, commerceAbility * GlobalVariables.internalGrowthFactor, this.getKind().getCommerce());

        float technologyAbility = (mayor == null ? 0 : mayor.getTechnologyAbility()) * GlobalVariables.mayorInternalWorkEfficiency +
                technologyWorkingPersons.getAll().stream()
                        .map(p -> (float) p.getTechnologyAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor));
        this.technology = Utility.diminishingGrowth(
                this.technology, technologyAbility * GlobalVariables.internalGrowthFactor, this.getKind().getTechnology());

        float moraleAbility = (mayor == null ? 0 : mayor.getMoraleAbility()) * GlobalVariables.mayorInternalWorkEfficiency +
                moraleWorkingPersons.getAll().stream()
                        .map(p -> (float) p.getMoraleAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor));
        this.morale = Utility.diminishingGrowth(
                this.morale, moraleAbility * GlobalVariables.internalGrowthFactor, this.getKind().getMorale());

        float enduranceAbility = (mayor == null ? 0 : mayor.getEnduranceAbility()) * GlobalVariables.mayorInternalWorkEfficiency +
                enduranceWorkingPersons.getAll().stream()
                        .map(p -> (float) p.getEnduranceAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor));
        this.endurance = Utility.diminishingGrowth(
                this.endurance, enduranceAbility * GlobalVariables.internalGrowthFactor, this.getKind().getEndurance());
    }

    @LuaAI.ExportToLua
    public double getFundGainedNextMonth() {
        return GlobalVariables.gainFund * (this.commerce + this.population * GlobalVariables.gainFundPerPopulation);
    }

    @LuaAI.ExportToLua
    public double getFoodGainedNextMonth() {
        return GlobalVariables.gainFood * (this.agriculture + this.population * GlobalVariables.gainFoodPerPopulation);
    }

    private void gainResources() {
        this.fund = (int) MathUtils.clamp(this.fund + getFundGainedNextMonth(), 0, this.getKind().getMaxFund());
        this.food = (int) MathUtils.clamp(this.food + getFoodGainedNextMonth(), 0, this.getKind().getMaxFood());
    }

    private void recruitMilitaries() {
        GameObjectList<Person> recruitWorkingPersons = getWorkingPersons(Person.DoingWork.RECRUIT);
        GameObjectList<Military> toRecruit = getRecruitableMilitaries();

        if (recruitWorkingPersons.size() <= 0 || toRecruit.size() <= 0 || this.population <= 0) {
            recruitWorkingPersons.forEach(p -> p.setDoingWork(Person.DoingWork.NONE));
            return;
        }

        GameObjectList<Person> recruitedPersons = new GameObjectList<>();
        {
            GameObjectList<Military> toRecruitByLeader = toRecruit.filter(m -> m.getLeader() != null);
            List<Military> costMilitary = toRecruitByLeader.sort((a, b) -> {
                float costA = a.getKind().getCost(this) * GlobalVariables.recruitCostFactor;
                float costB = b.getKind().getCost(this) * GlobalVariables.recruitCostFactor;
                return Float.compare(costA, costB);
            });
            int runningCost = 0;
            List<Military> actualToRecruit = new ArrayList<>();
            for (Military m : costMilitary) {
                if (m.getLeader().getDoingWorkType() != Person.DoingWork.RECRUIT &&
                        m.getLeader().getDoingWorkType() != Person.DoingWork.MAYOR) continue;
                int newCost = Math.round(m.getKind().getCost(this) * GlobalVariables.recruitCostFactor);
                if (runningCost + newCost > fund) {
                    break;
                } else {
                    runningCost += newCost;
                    actualToRecruit.add(m);
                }
            }

            if (actualToRecruit.size() > 0) {
                for (Military m : actualToRecruit) {
                    recruitedPersons.add(m.getLeader());

                    float recruited = m.getLeader().getRecruitAbility() * GlobalVariables.recruitByLeaderEfficiency;
                    int thisRecruited = Math.round(recruited / actualToRecruit.size() * m.getKind().getUnitQuantity());
                    thisRecruited = Math.min(population, thisRecruited);
                    m.increaseQuantity(thisRecruited, GlobalVariables.recruitMorale, GlobalVariables.recruitCombativity);
                    loseFund(Math.round(m.getKind().getCost(this) * GlobalVariables.recruitCostFactor));
                    losePopulation(thisRecruited);

                    if (this.population <= 0) {
                        recruitWorkingPersons.forEach(p -> p.setDoingWork(Person.DoingWork.NONE));
                        return;
                    }
                }
            }
        }

        recruitWorkingPersons.remove(recruitedPersons::contains);
        toRecruit.remove(m -> !m.recruitable() || m.getLeader() != null);

        List<Military> costMilitary = toRecruit.sort((a, b) -> {
            float costA = a.getKind().getCost(this) * GlobalVariables.recruitCostFactor;
            float costB = b.getKind().getCost(this) * GlobalVariables.recruitCostFactor;
            return Float.compare(costA, costB);
        });
        int runningCost = 0;
        List<Military> actualToRecruit = new ArrayList<>();
        for (Military m : costMilitary) {
            int newCost = Math.round(m.getKind().getCost(this) * GlobalVariables.recruitCostFactor);
            if (runningCost + newCost > fund) {
                break;
            } else {
                runningCost += newCost;
                actualToRecruit.add(m);
            }
        }

        if (actualToRecruit.size() == 0) {
            recruitWorkingPersons.forEach(p -> p.setDoingWork(Person.DoingWork.NONE));
            return;
        }

        float recruitAbility = recruitWorkingPersons.getAll().stream()
                .map(p -> (float) p.getRecruitAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor))
                + getMayor().getRecruitAbility() * GlobalVariables.mayorInternalWorkEfficiency;
        float recruited = recruitAbility * GlobalVariables.recruitEfficiency;
        for (Military m : actualToRecruit) {
            int thisRecruited = Math.round(recruited / actualToRecruit.size() * m.getKind().getUnitQuantity());
            thisRecruited = Math.min(population, thisRecruited);
            m.increaseQuantity(thisRecruited, GlobalVariables.recruitMorale, GlobalVariables.recruitCombativity);
            loseFund(Math.round(m.getKind().getCost(this) * GlobalVariables.recruitCostFactor));
            losePopulation(thisRecruited);

            if (this.population <= 0) {
                recruitWorkingPersons.forEach(p -> p.setDoingWork(Person.DoingWork.NONE));
                break;
            }
        }
    }

    private void trainMilitaries() {
        GameObjectList<Person> trainWorkingPersons = getWorkingPersons(Person.DoingWork.TRAINING);
        GameObjectList<Military> toTrain = getTrainableMilitaries();

        if (trainWorkingPersons.size() <= 0 || toTrain.size() <= 0) {
            trainWorkingPersons.forEach(p -> p.setDoingWork(Person.DoingWork.NONE));
            return;
        }

        GameObjectList<Person> trainedPersons = new GameObjectList<>();
        {
            GameObjectList<Military> trainByLeader = toTrain.filter(m -> m.getLeader() != null);
            for (Military m : trainByLeader) {
                trainedPersons.add(m.getLeader());

                float trained = m.getLeader().getTrainingAbility() * GlobalVariables.trainByLeaderEfficiency;
                int thisTrained = Math.round(trained / m.getUnitCount() / toTrain.size());
                m.increaseCombativity(thisTrained);
                m.increaseMorale((int) (thisTrained * GlobalVariables.moraleTrainFactor));
            }
        }

        trainWorkingPersons.remove(trainedPersons::contains);
        toTrain.remove(m -> !m.trainable() || m.getLeader() != null);

        float trainAbility = trainWorkingPersons.getAll().stream()
                .map(p -> (float) p.getTrainingAbility()).collect(Utility.diminishingSum(GlobalVariables.internalPersonDiminishingFactor))
                + getMayor().getTrainingAbility() * GlobalVariables.mayorInternalWorkEfficiency;
        float trained = trainAbility * GlobalVariables.trainEfficiency;
        for (Military m : toTrain) {
            int thisTrained = Math.round(trained / m.getUnitCount() / toTrain.size());
            m.increaseCombativity(thisTrained);
            m.increaseMorale((int) (thisTrained * GlobalVariables.moraleTrainFactor));
        }
    }

    public float getDefense() {
        return (float) (Math.max(GlobalVariables.architectureMinCommand, getMayor() == null ? 0 : getMayor().getCommand()) / 100.0f
                        * Math.pow(getMorale(), GlobalVariables.architectureDefenseMoralePower)
                        * Math.pow(getEndurance(), GlobalVariables.architectureDefenseEndurancePower));
    }

    public boolean canRecallPerson() {
        return this.getBelongedFaction().getArchitectures().size() > 1 && getRecallablePersonList().size() > 0;
    }

    public boolean canMovePerson() {
        return this.getBelongedFaction().getArchitectures().size() > 1 && this.getPersons().size() > 0;
    }

    public GameObjectList<Person> getRecallablePersonList() {
        return getBelongedFaction().getPersons().filter(x -> x.getState() == Person.State.NORMAL
                && x.getLocation() != this && x.getLocation() instanceof Architecture);
    }

    void addConnectedArchitectures(Architecture a) {
        connectedArchitectures.add(a.getId());
    }

    @LuaAI.ExportToLua
    public GameObjectList<Architecture> getConnectedArchitectures() {
        return scenario.getArchitectures().getItemsFromIds(connectedArchitectures);
    }

    @LuaAI.ExportToLua
    public GameObjectList<Architecture> getHostileConnectedArchitectures() {
        return scenario.getArchitectures().filter(a -> a.getBelongedFaction() != this.getBelongedFaction()).getItemsFromIds(connectedArchitectures);
    }

    @LuaAI.ExportToLua
    public boolean isFrontline() {
        return scenario.getArchitectures().getAll().stream().anyMatch(a -> a.getBelongedFaction() != this.getBelongedFaction());
    }

    @LuaAI.ExportToLua
    public GameObjectList<Troop> getFriendlyTroopsInView() {
        return scenario.getTroops().filter(t -> t.getPosition().taxiDistanceTo(this.getPosition()) <= 5 && t.getBelongedFaction() == this.getBelongedFaction());
    }

    @LuaAI.ExportToLua
    public GameObjectList<Troop> getHostileTroopsInView() {
        return scenario.getTroops().filter(t -> t.getPosition().taxiDistanceTo(this.getPosition()) <= 5 && t.getBelongedFaction() != this.getBelongedFaction());
    }

}
