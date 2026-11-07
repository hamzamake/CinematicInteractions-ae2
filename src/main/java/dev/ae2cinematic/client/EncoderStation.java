package dev.ae2cinematic.client;

import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Pattern Encoding Terminal. The purple screen is covered by a dark panel: a ghost crafting grid on the left, an arrow
 * to the recipe's result box in the middle, the pattern box on the right. Blank patterns get smacked into the box; the encode
 * button sends the grid's ingredients flying into the box, which flashes and pushes the encoded pattern out on top.
 */
final class EncoderStation extends SlotStation<PatternEncodingTermMenu> {
    private static final double SMACK_WIND = 0.14, SMACK_HIT = 0.2, SMACK_LENGTH = 0.35;
    private static final double PRESS = 0.18, FLY_START = 0.08, FLY_STAGGER = 0.025, FLY_TIME = 0.35;
    private static final double ENCODE_AT = 0.65, ENCODE_LENGTH = 1.15;

    private double smackAt = -10;
    private Spot smackSpot;
    private int smackButton;
    private boolean smackLanded = true;
    private double encodeAt = -10;
    private boolean encodeSent = true;
    private final List<ItemStack> flying = new ArrayList<>(9);
    private final List<Vec3> flyingFrom = new ArrayList<>(9);
    private boolean craftingMode;

    EncoderStation(BlockPos pos, Direction side) {
        super(pos, side, PatternEncodingTermMenu.class, "pattern_encoding_terminal", UP);
    }

    // layout on the face (u right, v up): grid | arrow | result | pattern box
    private static final double GRID_U = -0.215, GRID_V = 0.06, CELL = 0.1, RESULT_U = 0.035, BOX_U = 0.25, BOX_V = -0.03;

    private Vec3 cell(int i) {
        return at(GRID_U + (i % 3 - 1) * CELL, GRID_V - (i / 3 - 1) * CELL, 0.012);
    }

    private Vec3 resultPoint() {
        return at(RESULT_U, GRID_V, 0.02);
    }

    private Vec3 boxCenter() {
        return at(BOX_U, BOX_V, 0.05);
    }

    private Vec3 boxFront() {
        return at(BOX_U, BOX_V, 0.1);
    }

    private Vec3 slit() {
        return boxCenter().add(up.scale(0.08));
    }

    private Vec3 encodedPoint() {
        return at(BOX_U, 0.24, 0.04);
    }

    private Vec3 button() {
        return at(BOX_U, -0.27, 0.012);
    }

    @Override
    protected List<Spot> layout(PatternEncodingTermMenu menu) {
        List<Spot> spots = new ArrayList<>(12);
        FakeSlot[] grid = menu.getCraftingGridSlots();
        for (int i = 0; i < grid.length && i < 9; i++) {
            spots.add(Spot.of(grid[i], cell(i), 0.042, 0.042, false));
        }
        Slot blank = first(menu, SlotSemantics.BLANK_PATTERN), encoded = first(menu, SlotSemantics.ENCODED_PATTERN);
        if (blank != null) {
            spots.add(Spot.of(blank, boxFront(), 0.09, 0.08, false));
        }
        if (encoded != null) {
            spots.add(Spot.of(encoded, encodedPoint(), 0.06, 0.06, false));
        }
        spots.add(Spot.button("result", resultPoint(), 0.055, 0.055));
        spots.add(Spot.button("encode", button(), 0.08, 0.034));
        return spots;
    }

    @Override
    public void beforeFrame() {
        super.beforeFrame();
        PatternEncodingTermMenu menu = menu();
        if (menu == null) {
            return;
        }
        if (!craftingMode) {
            // this scene is the crafting-table layout, so put the terminal in crafting mode
            craftingMode = true;
            if (menu.getMode() != EncodingMode.CRAFTING) {
                menu.setMode(EncodingMode.CRAFTING);
            }
        }
        double now = Scene.clock();
        if (!smackLanded && now - smackAt >= SMACK_HIT) {
            smackLanded = true;
            slotClick(smackSpot.slot().index, smackButton, false);
            kick(0.85);
            sound(SoundEvents.ANVIL_LAND, 0.22f, 1.8f);
            sound(SoundEvents.WOOD_HIT, 0.8f, 0.8f);
        }
        if (!encodeSent && now - encodeAt >= ENCODE_AT) {
            encodeSent = true;
            menu.encode();
            kick(0.55);
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            sound(SoundEvents.BEACON_POWER_SELECT, 0.35f, 1.9f);
        }
    }

    @Override
    protected void place(Spot s, int button) {
        if (s.slot() == first(menu(), SlotSemantics.BLANK_PATTERN)) {
            smackSpot = s;
            smackButton = button;
            smackAt = Scene.clock();
            smackLanded = false;
            return;
        }
        super.place(s, button);
    }

    @Override
    protected void press(String button) {
        PatternEncodingTermMenu menu = menu();
        if ("result".equals(button)) {
            return; // just shows what the recipe makes
        }
        if (encoding() || !canEncode(menu)) {
            deny();
            return;
        }
        encodeAt = Scene.clock();
        encodeSent = false;
        flying.clear();
        flyingFrom.clear();
        FakeSlot[] grid = menu.getCraftingGridSlots();
        for (int i = 0; i < grid.length && i < 9; i++) {
            if (grid[i].hasItem()) {
                flying.add(grid[i].getItem().copy());
                flyingFrom.add(cell(i));
            }
        }
        sound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.2f);
    }

    private static boolean canEncode(PatternEncodingTermMenu menu) {
        Slot result = first(menu, SlotSemantics.CRAFTING_RESULT);
        Slot blank = first(menu, SlotSemantics.BLANK_PATTERN), encoded = first(menu, SlotSemantics.ENCODED_PATTERN);
        boolean recipe = result != null && result.hasItem();
        boolean pattern = blank != null && blank.hasItem() || encoded != null && encoded.hasItem();
        return recipe && pattern;
    }

    private boolean encoding() {
        return Scene.clock() - encodeAt < ENCODE_LENGTH;
    }

    private boolean smacking() {
        return Scene.clock() - smackAt < SMACK_LENGTH;
    }

    @Override
    protected Vec3 animatedAim() {
        double s = Scene.clock() - smackAt;
        if (s >= 0 && s < SMACK_LENGTH) {
            if (s < SMACK_WIND) {
                return slit().add(up.scale(0.12)).add(normal.scale(0.2)); // wind up, high and back
            }
            return boxFront().add(normal.scale(s < SMACK_HIT ? 0.0 : 0.04));
        }
        double e = Scene.clock() - encodeAt;
        if (e >= 0 && e < PRESS) {
            return button().add(normal.scale(0.005));
        }
        return super.animatedAim();
    }

    @Override
    public double handsStiffness() {
        return smacking() ? 75 : super.handsStiffness();
    }

    @Override
    public boolean busy() {
        return super.busy() || smacking() || encoding();
    }

    @Override
    public double flash() {
        double t = Scene.clock() - encodeAt - ENCODE_AT;
        return t >= 0 && t < 1 ? 0.45 * Math.exp(-7 * t) : 0;
    }

    @Override
    public int flashColor() {
        return 0x8FE7FF;
    }

    @Override
    protected List<Component> buttonTooltip(String button) {
        PatternEncodingTermMenu menu = menu();
        Slot result = first(menu, SlotSemantics.CRAFTING_RESULT);
        if ("result".equals(button)) {
            return result != null && result.hasItem() ? describe(result.getItem()) : List.of(tr("encoder.no_recipe").withStyle(ChatFormatting.GRAY));
        }
        if (result == null || !result.hasItem()) {
            return List.of(tr("encoder.no_recipe").withStyle(ChatFormatting.RED));
        }
        if (!canEncode(menu)) {
            return List.of(tr("encoder.no_blank").withStyle(ChatFormatting.RED));
        }
        return List.of(tr("encoder.encode", result.getItem().getHoverName()).withStyle(ChatFormatting.AQUA),
                tr("button.encode").withStyle(ChatFormatting.GRAY));
    }

    // ---- drawing ---------------------------------------------------------------------------------------------------

    @Override
    protected void drawExtras(PoseStack ps, MultiBufferSource buffers, Vec3 cam, int light) {
        PatternEncodingTermMenu menu = menu();
        double now = Scene.clock();

        // the screen, covered: grid | arrow | result box | pattern box
        panel(ps, buffers, cam, 0, 0, 0.375, 0.375, 0.004, 0xFF1B1D22);
        panel(ps, buffers, cam, GRID_U, GRID_V, 0.16, 0.16, 0.006, 0xFF4A5263);
        panel(ps, buffers, cam, GRID_U, GRID_V, 0.152, 0.152, 0.007, 0xFF23262E);
        for (int i = 0; i < 9; i++) {
            panel(ps, buffers, cam, GRID_U + (i % 3 - 1) * CELL, GRID_V - (i / 3 - 1) * CELL, 0.042, 0.042, 0.008, 0xFF3A3E48);
        }
        label(ps, buffers, cam, tr("encoder.grid"), at(GRID_U, GRID_V + 0.195, 0.009), 0xFFB8C2D6, 0xF000F0);
        // arrow
        Vec3 a0 = at(-0.065, GRID_V, 0.008);
        quad(ps, buffers, cam, a0.add(up.scale(0.012)).subtract(across.scale(0.035)), a0.add(up.scale(0.012)), a0.subtract(up.scale(0.012)),
                a0.subtract(up.scale(0.012)).subtract(across.scale(0.035)), 0xFF8A93A6);
        quad(ps, buffers, cam, a0.add(up.scale(0.03)), a0.add(across.scale(0.03)), a0.add(across.scale(0.03)), a0.subtract(up.scale(0.03)), 0xFF8A93A6);
        // result box
        Slot result = first(menu, SlotSemantics.CRAFTING_RESULT);
        boolean hasResult = result != null && result.hasItem();
        panel(ps, buffers, cam, RESULT_U, GRID_V, 0.062, 0.062, 0.006, hasResult ? 0xFF3FA9C9 : 0xFF4A5263);
        panel(ps, buffers, cam, RESULT_U, GRID_V, 0.054, 0.054, 0.007, 0xFF15171C);
        label(ps, buffers, cam, tr("encoder.result"), at(RESULT_U, GRID_V + 0.095, 0.009), 0xFFB8C2D6, 0xF000F0);
        // encode button
        panel(ps, buffers, cam, BOX_U, -0.27, 0.08, 0.034, 0.008, canEncode(menu) ? 0xFF3FCF6A : 0xFF555A63);
        label(ps, buffers, cam, tr("encoder.button"), at(BOX_U, -0.27, 0.01), 0xFFFFFFFF, 0xF000F0);
        drawBox(ps, buffers, cam, now);

        FakeSlot[] grid = menu.getCraftingGridSlots();
        for (int i = 0; i < grid.length && i < 9; i++) {
            drawItem(ps, buffers, cam, grid[i].getItem(), cell(i).add(normal.scale(0.01)), 0.075f, light);
        }
        if (hasResult) {
            drawItem(ps, buffers, cam, result.getItem(), resultPoint().add(up.scale(0.006 * Math.sin(now * 3))), 0.09f, light);
        }
        Slot blank = first(menu, SlotSemantics.BLANK_PATTERN);
        if (blank != null && blank.hasItem()) {
            int shown = Math.min(3, blank.getItem().getCount());
            for (int i = 0; i < shown; i++) {
                drawItem(ps, buffers, cam, blank.getItem(), slit().add(up.scale(0.035)).add(across.scale((i - (shown - 1) / 2.0) * 0.03)), 0.075f, light);
            }
        }
        Slot encoded = first(menu, SlotSemantics.ENCODED_PATTERN);
        if (encoded != null && encoded.hasItem()) {
            double rise = Anim.outBack(Anim.clamp01((now - encodeAt - ENCODE_AT) / (ENCODE_LENGTH - ENCODE_AT)), 1.6);
            Vec3 at = encoding() ? Anim.lerp(slit(), encodedPoint(), rise) : encodedPoint().add(up.scale(0.012 * Math.sin(now * 2.5)));
            drawItem(ps, buffers, cam, encoded.getItem(), at, 0.1f, light);
        }
        // the ingredients fly from the grid into the box
        for (int k = 0; k < flying.size(); k++) {
            double t = (now - encodeAt - FLY_START - k * FLY_STAGGER) / FLY_TIME;
            if (t < 0 || t >= 1 || encodeSent && now - encodeAt > ENCODE_AT) {
                continue;
            }
            double e = Anim.inOutCubic(t);
            Vec3 at = Anim.lerp(flyingFrom.get(k), slit(), e).add(normal.scale(0.08 * Anim.hump(t))).add(up.scale(0.1 * Anim.hump(t)));
            drawItem(ps, buffers, cam, flying.get(k), at, (float) (0.085 * (1 - 0.6 * e)), light);
        }
    }

    private void drawBox(PoseStack ps, MultiBufferSource buffers, Vec3 cam, double now) {
        double s = now - smackAt, e = now - encodeAt - ENCODE_AT;
        // squashes when smacked, swells when it encodes
        double squash = s >= 0 && s < 0.4 && s > SMACK_HIT ? 0.18 * Math.exp(-14 * (s - SMACK_HIT)) : 0;
        double swell = e >= 0 && e < 0.5 ? 0.12 * Math.sin(Math.PI * e / 0.5) : 0;
        Vec3 c = boxCenter();
        Vec3 x = across.scale(0.09 * (1 + swell + squash * 0.5)), y = up.scale(0.08 * (1 + swell - squash)), z = normal.scale(0.05);
        quad(ps, buffers, cam, c.add(z).subtract(x).subtract(y), c.add(z).add(x).subtract(y), c.add(z).add(x).add(y), c.add(z).subtract(x).add(y), 0xFF4A505C);
        quad(ps, buffers, cam, c.add(y).subtract(x).subtract(z), c.add(y).add(x).subtract(z), c.add(y).add(x).add(z), c.add(y).subtract(x).add(z), 0xFF5C6370);
        quad(ps, buffers, cam, c.subtract(y).subtract(x).subtract(z), c.subtract(y).add(x).subtract(z), c.subtract(y).add(x).add(z), c.subtract(y).subtract(x).add(z), 0xFF30343C);
        quad(ps, buffers, cam, c.add(x).subtract(y).subtract(z), c.add(x).add(y).subtract(z), c.add(x).add(y).add(z), c.add(x).subtract(y).add(z), 0xFF3C414B);
        quad(ps, buffers, cam, c.subtract(x).subtract(y).subtract(z), c.subtract(x).add(y).subtract(z), c.subtract(x).add(y).add(z), c.subtract(x).subtract(y).add(z), 0xFF3C414B);
        // the slit on top and a stripe on the front that lights up while it encodes
        Vec3 top = c.add(y.scale(1.01)), sx = across.scale(0.06), sz = normal.scale(0.012);
        quad(ps, buffers, cam, top.subtract(sx).subtract(sz), top.add(sx).subtract(sz), top.add(sx).add(sz), top.subtract(sx).add(sz), 0xFF101114);
        int glow = e >= -ENCODE_AT && e < 0.5 ? 0xFF8FE7FF : 0xFF2F7F99;
        Vec3 front = c.add(z.scale(1.02)).subtract(y.scale(0.6)), fx = across.scale(0.065), fy = up.scale(0.01);
        quad(ps, buffers, cam, front.subtract(fx).subtract(fy), front.add(fx).subtract(fy), front.add(fx).add(fy), front.subtract(fx).add(fy), glow);
    }
}
