"""
Create Better Sharingan Animation - Match the quality of the GIF preview
Using proper tomoe shapes that look like the preview
"""

import json
import math

def create_beautiful_sharingan():
    """Create a Sharingan animation that matches the GIF preview quality"""
    
    lottie = {
        "v": "5.9.0",
        "fr": 30,
        "ip": 0,
        "op": 90,
        "w": 512,
        "h": 512,
        "nm": "Beautiful Mangekyo Sharingan",
        "ddd": 0,
        "assets": [],
        "layers": []
    }
    
    center = 256
    
    # Layer ordering: lower index = on top
    
    # Layer 1: Pupil highlight (TOP)
    lottie["layers"].append({
        "ddd": 0,
        "ind": 1,
        "ty": 4,
        "nm": "Pupil Highlight",
        "sr": 1,
        "ks": {
            "o": {"a": 0, "k": 60},
            "r": {"a": 0, "k": 0},
            "p": {"a": 0, "k": [center - 15, center + 15, 0]},
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": {"a": 0, "k": [100, 100, 100]}
        },
        "ao": 0,
        "shapes": [{
            "ty": "gr",
            "it": [
                {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [30, 30]}},
                {"ty": "fl", "c": {"a": 0, "k": [1, 1, 1, 1]}, "o": {"a": 0, "k": 100}},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
            ]
        }],
        "ip": 0, "op": 90, "st": 0, "bm": 0
    })
    
    # Layer 2: Center pupil
    lottie["layers"].append({
        "ddd": 0,
        "ind": 2,
        "ty": 4,
        "nm": "Pupil",
        "sr": 1,
        "ks": {
            "o": {"a": 0, "k": 100},
            "r": {"a": 0, "k": 0},
            "p": {"a": 0, "k": [center, center, 0]},
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": {"a": 0, "k": [100, 100, 100]}
        },
        "ao": 0,
        "shapes": [{
            "ty": "gr",
            "it": [
                {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [80, 80]}},
                {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
            ]
        }],
        "ip": 0, "op": 90, "st": 0, "bm": 0
    })
    
    # Layers 3-5: Three tomoe (comma shapes) - like in the GIF
    for i in range(3):
        angle = i * 120
        
        # Calculate tomoe position
        rad = math.radians(angle)
        distance = 120
        tx = distance * math.cos(rad)
        ty = distance * math.sin(rad)
        
        # Create tomoe with proper comma shape
        lottie["layers"].append({
            "ddd": 0,
            "ind": 3 + i,
            "ty": 4,
            "nm": f"Tomoe {i + 1}",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": 100},
                "r": {
                    "a": 1,
                    "k": [
                        {"i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}, "t": 0, "s": [angle]},
                        {"t": 90, "s": [angle + 360]}
                    ]
                },
                "p": {"a": 0, "k": [center, center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [
                # Main tomoe body (large circle)
                {
                    "ty": "gr",
                    "it": [
                        {"ty": "el", "p": {"a": 0, "k": [tx, ty]}, "s": {"a": 0, "k": [70, 70]}},
                        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                        {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                    ],
                    "nm": "Tomoe Body"
                },
                # Tail circles to create comma shape
                {
                    "ty": "gr",
                    "it": [
                        {"ty": "el", "p": {"a": 0, "k": [tx + 25 * math.cos(math.radians(angle + 180)), ty + 25 * math.sin(math.radians(angle + 180))]}, "s": {"a": 0, "k": [60, 60]}},
                        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                        {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                    ],
                    "nm": "Tail 1"
                },
                {
                    "ty": "gr",
                    "it": [
                        {"ty": "el", "p": {"a": 0, "k": [tx + 45 * math.cos(math.radians(angle + 190)), ty + 45 * math.sin(math.radians(angle + 190))]}, "s": {"a": 0, "k": [48, 48]}},
                        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                        {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                    ],
                    "nm": "Tail 2"
                },
                {
                    "ty": "gr",
                    "it": [
                        {"ty": "el", "p": {"a": 0, "k": [tx + 60 * math.cos(math.radians(angle + 205)), ty + 60 * math.sin(math.radians(angle + 205))]}, "s": {"a": 0, "k": [35, 35]}},
                        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                        {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                    ],
                    "nm": "Tail 3"
                },
                {
                    "ty": "gr",
                    "it": [
                        {"ty": "el", "p": {"a": 0, "k": [tx + 72 * math.cos(math.radians(angle + 220)), ty + 72 * math.sin(math.radians(angle + 220))]}, "s": {"a": 0, "k": [22, 22]}},
                        {"ty": "fl", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                        {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                    ],
                    "nm": "Tail 4"
                }
            ],
            "ip": 0, "op": 90, "st": 0, "bm": 0
        })
    
    # Layer 6: Inner ring around tomoe
    lottie["layers"].append({
        "ddd": 0,
        "ind": 6,
        "ty": 4,
        "nm": "Inner Ring",
        "sr": 1,
        "ks": {
            "o": {"a": 0, "k": 100},
            "r": {"a": 0, "k": 0},
            "p": {"a": 0, "k": [center, center, 0]},
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": {"a": 0, "k": [100, 100, 100]}
        },
        "ao": 0,
        "shapes": [{
            "ty": "gr",
            "it": [
                {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [200, 200]}},
                {"ty": "st", "c": {"a": 0, "k": [0.545, 0, 0, 1]}, "o": {"a": 0, "k": 100}, "w": {"a": 0, "k": 6}, "lc": 1, "lj": 1},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
            ]
        }],
        "ip": 0, "op": 90, "st": 0, "bm": 0
    })
    
    # Layer 7: Gradient circles for depth (multiple layers)
    for i, (radius, alpha) in enumerate([(180, 0.3), (160, 0.2), (140, 0.15)]):
        lottie["layers"].append({
            "ddd": 0,
            "ind": 7 + i,
            "ty": 4,
            "nm": f"Depth {i + 1}",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": alpha * 100},
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [center, center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [{
                "ty": "gr",
                "it": [
                    {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [radius * 2, radius * 2]}},
                    {"ty": "fl", "c": {"a": 0, "k": [0.545, 0, 0, 1]}, "o": {"a": 0, "k": 100}},
                    {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
                ]
            }],
            "ip": 0, "op": 90, "st": 0, "bm": 0
        })
    
    # Layer 10: Main red iris
    lottie["layers"].append({
        "ddd": 0,
        "ind": 10,
        "ty": 4,
        "nm": "Red Iris",
        "sr": 1,
        "ks": {
            "o": {"a": 0, "k": 100},
            "r": {"a": 0, "k": 0},
            "p": {"a": 0, "k": [center, center, 0]},
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": {"a": 0, "k": [100, 100, 100]}
        },
        "ao": 0,
        "shapes": [{
            "ty": "gr",
            "it": [
                {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [400, 400]}},
                {"ty": "fl", "c": {"a": 0, "k": [0.769, 0.118, 0.227, 1]}, "o": {"a": 0, "k": 100}},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
            ]
        }],
        "ip": 0, "op": 90, "st": 0, "bm": 0
    })
    
    # Layer 11: Outer black ring (BOTTOM)
    lottie["layers"].append({
        "ddd": 0,
        "ind": 11,
        "ty": 4,
        "nm": "Outer Ring",
        "sr": 1,
        "ks": {
            "o": {"a": 0, "k": 100},
            "r": {"a": 0, "k": 0},
            "p": {"a": 0, "k": [center, center, 0]},
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": {"a": 0, "k": [100, 100, 100]}
        },
        "ao": 0,
        "shapes": [{
            "ty": "gr",
            "it": [
                {"ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [440, 440]}},
                {"ty": "st", "c": {"a": 0, "k": [0, 0, 0, 1]}, "o": {"a": 0, "k": 100}, "w": {"a": 0, "k": 16}, "lc": 1, "lj": 1},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]}, "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]}, "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}, "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0}, "nm": "Transform"}
            ]
        }],
        "ip": 0, "op": 90, "st": 0, "bm": 0
    })
    
    return lottie


def save_json(data, filename):
    """Save JSON"""
    with open(filename, 'w') as f:
        json.dump(data, f, separators=(',', ':'))
    
    import os
    size_kb = os.path.getsize(filename) / 1024
    print(f"✓ Saved {filename} - Size: {size_kb:.2f} KB")


def main():
    print("=" * 70)
    print("🔥 CREATING BEAUTIFUL SHARINGAN ANIMATION 🔥")
    print("=" * 70)
    print()
    print("Creating animation that matches the GIF preview quality...")
    print()
    print("Features:")
    print("  ✓ Three rotating tomoe (comma shapes)")
    print("  ✓ Proper tail curves")
    print("  ✓ Depth gradient layers")
    print("  ✓ Inner ring around tomoe area")
    print("  ✓ Red Sharingan color (#C41E3A)")
    print("  ✓ Black outer ring")
    print("  ✓ Center pupil with highlight")
    print()
    
    sharingan = create_beautiful_sharingan()
    save_json(sharingan, "mangekyo_itachi_fixed.json")
    
    print()
    print("=" * 70)
    print("✅ BEAUTIFUL ANIMATION CREATED!")
    print("=" * 70)
    print()
    print("Next steps:")
    print("  Copy-Item mangekyo_itachi_fixed.json app/src/main/assets/animations/mangekyo_itachi.json -Force")
    print("  .\\gradlew.bat installPlaystoreDebug")
    print()


if __name__ == "__main__":
    main()
