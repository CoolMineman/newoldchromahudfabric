/*
 *       Copyright (C) 2018-present Hyperium <https://hyperium.cc/>
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published
 *       by the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cc.hyperium.mods.chromahud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mitchell Katz on 5/25/2017.
 */
public class ElementRenderer {

    private static final List<Long> clicks = new ArrayList<>();
    private static final List<Long> rClicks = new ArrayList<>();
    private static double currentScale = 1.0;
    private static int color;
    private static DisplayElement current;
    private static TextRenderer fontRendererObj = MinecraftClient.getInstance().textRenderer;
    private static String cValue;
    private final ChromaHUD mod;
    private final MinecraftClient minecraft;
    private boolean last;
    private boolean rLast;

    private static ItemRenderer renderItem = new ItemRenderer();

    public ElementRenderer(ChromaHUD mod) {
        this.mod = mod;
        minecraft = MinecraftClient.getInstance();
    }

    public static String getCValue() {
        return cValue;
    }

    public static double getCurrentScale() {
        return currentScale;
    }

    public static int getColor(int c) {
        return c;
    }

    public static void draw(int x, double y, String string) {
        List<String> tmp = new ArrayList<>();
        tmp.add(string);
        draw(x, y, tmp);
    }

    public static void draw(int x, double y, List<String> list) {
        double ty = y;
        for (String string : list) {
            int shift = current.isRightSided()
                ? fontRendererObj.getStringWidth(string)
                : 0;

            if (current.isHighlighted()) {
                int stringWidth = fontRendererObj.getStringWidth(string);
                DrawableHelper.fill((int) ((x - 1) / currentScale - shift), (int) ((ty - 1) / currentScale), (int) ((x + 1) / currentScale)
                    + stringWidth - shift, (int) ((ty + 1) / currentScale) + 8, new Color(0, 0, 0, 120).getRGB());
            }

            if (current.isChroma()) {
                drawChromaString(string, x - shift, (int) ty);
            } else {
                draw(string, (int) (x / currentScale - shift), (int) (ty / currentScale), getColor(color), current.isShadow());
            }

            ty += 10D * currentScale;
        }
    }

    public static void draw(String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            fontRendererObj.draw(text, x, y, color);
        } else {
            fontRendererObj.drawWithShadow(text, x, y, color);
        }
    }

    // Don't shift, by the time it is here it is already shifted
    private static void drawChromaString(String text, int xIn, int y) {
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int x = xIn;

        for (char c : text.toCharArray()) {
            long dif = (x * 10) - (y * 10);
            if (current.isStaticChroma()) dif = 0;
            long l = System.currentTimeMillis() - dif;
            float ff = current.isStaticChroma() ? 1000.0F : 2000.0F;
            int i = Color.HSBtoRGB((float) (l % (int) ff) / ff, 0.8F, 0.8F);
            String tmp = String.valueOf(c);
            draw(tmp, (int) ((double) x / currentScale), (int) ((double) y / currentScale), i, current.isShadow());
            x += (double) renderer.getStringWidth(String.valueOf(c)) * currentScale;
        }
    }


    public static int maxWidth(List<String> list) {
        int max = 0;

        for (String s : list) {
            max = Math.max(max, MinecraftClient.getInstance().textRenderer.getStringWidth(s));
        }

        return max;
    }

    public static int getColor() {
        return color;
    }

    public static int getCPS() {
        clicks.removeIf(aLong -> System.currentTimeMillis() - aLong > 1000L);
        return clicks.size();
    }

    public static DisplayElement getCurrent() {
        return current;
    }

    public static void render(List<ItemStack> itemStacks, int x, double y, boolean showDurability) {
        GL11.glPushMatrix();
        int line = 0;

        for (ItemStack stack : itemStacks) {
            if (stack.getMaxDamage() == 0) continue;
            String dur = stack.getMaxDamage() - stack.getDamage() + "";
            renderItem.renderStack(MinecraftClient.getInstance().textRenderer, MinecraftClient.getInstance().getTextureManager(), stack, (int) (x / ElementRenderer.getCurrentScale() - (current.isRightSided() ? (showDurability ? currentScale + fontRendererObj.getStringWidth(dur) : -8) : 0)), (int) ((y + (16 * line * ElementRenderer.getCurrentScale())) / ElementRenderer.getCurrentScale()));
            if (showDurability) ElementRenderer.draw((int) (x + (double) 20 * currentScale), y + (16 * line) + 4, dur);
            line++;
        }

        GL11.glPopMatrix();
    }

    public static void startDrawing(DisplayElement element) {
        GL11.glPushMatrix();
        GL11.glScaled(element.getScale(), element.getScale(), 1.0 / element.getScale());
        currentScale = element.getScale();
        color = element.getColor();
        current = element;
    }

    public static void endDrawing(DisplayElement element) {
        GL11.glScaled(1.0 / element.getScale(), 1.0 / element.getScale(), 1.0 / element.getScale());

        GL11.glPopMatrix();
    }


    public static TextRenderer getFontRenderer() {
        return fontRendererObj;
    }

    public static int getRightCPS() {
        rClicks.removeIf(aLong -> System.currentTimeMillis() - aLong > 1000L);
        return rClicks.size();
    }

    //@InvokeEvent
    public void tick() {
        if (MinecraftClient.getInstance().focused) {
            //todo 1.7.10 has diffrent debug
            // cValue = MinecraftClient.getInstance().worldRenderer.getChunksDebugString().split("/")[0].trim();
        }
    }

    // Right CPS Counter

    //@InvokeEvent
    public void onRenderTick() {
        if (!minecraft.focused || minecraft.options.field_9938) return;
        renderElements();
        // GlStateManager.clearColor();
    }

    private void renderElements() {
        if (fontRendererObj == null) fontRendererObj = MinecraftClient.getInstance().textRenderer;

        // Mouse Button Left
        boolean m = Mouse.isButtonDown(0);
        if (m != last) {
            last = m;
            if (m) clicks.add(System.currentTimeMillis());
        }

        // Mouse Button Right
        boolean rm = Mouse.isButtonDown(1);
        if (rm != rLast) {
            rLast = rm;
            if (rm) rClicks.add(System.currentTimeMillis());
        }

        // Others
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        List<DisplayElement> elementList = mod.getDisplayElements();
        elementList.forEach(element -> {
            startDrawing(element);
            try {
                element.draw();
            } catch (Exception ignored) {
            }
            endDrawing(element);
        });
    }
}
