# AE2 Cinematic Interactions

**Summary (for the CurseForge summary field):**
Cinematic Interactions for Applied Energistics 2: pull cells out of ME Drives, link terminals by touching the Wireless Access Point, smack patterns into the Pattern Encoding Terminal, and more. No more GUIs.

---

## What is this?

**AE2 Cinematic Interactions** is an addon that brings [Cinematic Interactions](https://www.curseforge.com/minecraft/mc-mods/cinematic-interactions) to **Applied Energistics 2**.

Instead of opening a flat GUI, right-clicking an AE2 machine walks you up to it. The camera focuses on the block, and you work it with your own hands, right in the world.

---

## Features

### 💾 ME Drive
- Right-click and the camera **focuses on the drive's front**.
- **Hover a bay** to read the cell inside:
  - what it stores (items, fluids, ...)
  - a **bytes-used bar** with used / total bytes
  - **types used** out of the maximum
  - the **top 6 things inside** and how many of each
- **Click a cell** and it slides out of the bay and drops into your inventory.
- **Inserting:** hold a cell, right-click the drive, then **click a free bay** and the cell slides in. Drag the mouse or scroll to push it in faster.

### ⚡ Charger
- A simple first-person animation: your hand reaches in to set the item down (or pick it up) and pulls back.

### 📡 Wireless Access Point
- **Touch the tip of the antenna** with a wireless terminal to link it. It chimes, flashes, and the terminal goes straight back into your hand.
- **Push Wireless Boosters up into the bottom** of the chassis. Click them to take them back out.
- The access point's **range and booster count** show at the top of the screen.

### 🧩 Pattern Encoding Terminal
- The terminal's screen becomes a workbench:
  - **Left:** a 3×3 **crafting grid**.
  - **Middle:** an arrow to a dedicated **result box** showing what the recipe makes.
  - **Right:** the **pattern box** with an **ENCODE** button.
- Hold an item and **click a grid square** to set it as an ingredient. Click with an empty hand to clear the square.
- **Smack blank patterns into the box.** Your hand winds up and slams them in, with camera shake.
- **Hit the encode button:** the ingredients fly out of the grid into the box, the box flashes, and the **encoded pattern pops out on top**. Click it to take it.

### 🔧 Also works with
| Block | What you do |
|---|---|
| **Inscriber** | Labelled sockets: **Top Press**, **Input** and **Bottom Press** stacked on the left, **Output** on the right |
| **ME IO Port** | Drag cells into the left bays; finished cells wait on the right |
| **Vibration Chamber** | Feed it fuel |
| **Matter Condenser** | Toss items in on top, drag a component into its bay, take the output |
| **Molecular Assembler** | Drag a pattern in |
| **Pattern Provider** | Slot encoded patterns into a 3×3 grid |

---

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.200+ |
| Applied Energistics 2 | 19.2.18+ |
| Cinematic Interactions | 0.18.30+ |
| GuideME | Any version AE2 accepts (AE2 already requires it) |

⚠️ **Install on both the client and the server.**

---

## Good to know
- **Sneak + right-click** still opens AE2's normal GUI, just like with Cinematic Interactions.
- On a cable, only the **Pattern Encoding Terminal** starts a scene. Every other part opens its normal AE2 GUI.
- Drives, Inscribers and Vibration Chambers that face **up or down** open the normal GUI, because you can only stand beside a block in a scene.
- All of your AE2 data stays the same: this mod only changes how you interact with the blocks.

---

## FAQ

**Does it work without Cinematic Interactions?**
No. This is an addon for Cinematic Interactions and needs it installed.

**Can I use it in my modpack?**
Yes, go ahead!

**Found a bug or a misplaced hotspot?**
Report it in the comments or the issue tracker. Please include a screenshot and your mod versions.

---

## Credits
- **Applied Energistics 2** by Team AppliedEnergistics
- **Cinematic Interactions** by Southware
- **AE2 Cinematic Interactions** by HamzaTom

License: MIT
