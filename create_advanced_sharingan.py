"""
Advanced Mangekyo Sharingan Animation - Professional Quality
Creates multiple Mangekyo patterns with smooth animations
Optimized Lottie output for mobile apps
"""

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Circle, Wedge, Polygon, PathPatch
from matplotlib.path import Path
import json
import math

class MangeykoSharingan:
    def __init__(self, size=512):
        self.size = size
        self.center = size / 2
        
    def create_lottie_animation(self, pattern="itachi", duration=3):
        """
        Create optimized Lottie animation
        Patterns: itachi, sasuke, kakashi, obito
        """
        
        fps = 30
        total_frames = int(duration * fps)
        
        lottie = {
            "v": "5.9.0",
            "fr": fps,
            "ip": 0,
            "op": total_frames,
            "w": self.size,
            "h": self.size,
            "nm": f"Mangekyo Sharingan - {pattern.title()}",
            "ddd": 0,
            "assets": [],
            "layers": []
        }
        
        # Background eye white
        lottie["layers"].append(self.create_eye_white_layer())
        
        # Iris background (red)
        lottie["layers"].append(self.create_iris_layer())
        
        # Pattern-specific rotating elements
        if pattern == "itachi":
            lottie["layers"].extend(self.create_itachi_pattern(total_frames))
        elif pattern == "sasuke":
            lottie["layers"].extend(self.create_sasuke_pattern(total_frames))
        elif pattern == "kakashi":
            lottie["layers"].extend(self.create_kakashi_pattern(total_frames))
        elif pattern == "obito":
            lottie["layers"].extend(self.create_obito_pattern(total_frames))
        
        # Central pupil
        lottie["layers"].append(self.create_pupil_layer())
        
        # Outer ring
        lottie["layers"].append(self.create_outer_ring_layer())
        
        return lottie
    
    def create_eye_white_layer(self):
        """Create white background of eye"""
        return {
            "ddd": 0,
            "ind": 1,
            "ty": 4,
            "nm": "Eye White",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": 100},
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [self.center, self.center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [{
                "ty": "gr",
                "it": [
                    {
                        "ty": "el",
                        "p": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [480, 480]}
                    },
                    {
                        "ty": "fl",
                        "c": {"a": 0, "k": [0.95, 0.95, 0.95, 1]},
                        "o": {"a": 0, "k": 100}
                    },
                    {
                        "ty": "tr",
                        "p": {"a": 0, "k": [0, 0]},
                        "a": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [100, 100]},
                        "r": {"a": 0, "k": 0},
                        "o": {"a": 0, "k": 100}
                    }
                ]
            }],
            "ip": 0,
            "op": 300,
            "st": 0,
            "bm": 0
        }
    
    def create_iris_layer(self):
        """Create red iris background"""
        return {
            "ddd": 0,
            "ind": 2,
            "ty": 4,
            "nm": "Iris",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": 100},
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [self.center, self.center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [{
                "ty": "gr",
                "it": [
                    {
                        "ty": "el",
                        "p": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [400, 400]}
                    },
                    {
                        "ty": "gf",  # Gradient fill
                        "o": {"a": 0, "k": 100},
                        "g": {
                            "p": 3,
                            "k": {
                                "a": 0,
                                "k": [0, 0.769, 0.118, 0.227, 0.5, 0.6, 0.05, 0.15, 1, 0.545, 0, 0]
                            }
                        },
                        "s": {"a": 0, "k": [0, 0]},
                        "e": {"a": 0, "k": [200, 0]},
                        "t": 1
                    },
                    {
                        "ty": "tr",
                        "p": {"a": 0, "k": [0, 0]},
                        "a": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [100, 100]},
                        "r": {"a": 0, "k": 0},
                        "o": {"a": 0, "k": 100}
                    }
                ]
            }],
            "ip": 0,
            "op": 300,
            "st": 0,
            "bm": 0
        }
    
    def create_itachi_pattern(self, total_frames):
        """Create Itachi's Mangekyo pattern (3-blade pinwheel)"""
        layers = []
        
        for i in range(3):
            angle = i * 120
            layer = {
                "ddd": 0,
                "ind": 10 + i,
                "ty": 4,
                "nm": f"Blade {i+1}",
                "sr": 1,
                "ks": {
                    "o": {"a": 0, "k": 100},
                    "r": {
                        "a": 1,
                        "k": [
                            {
                                "i": {"x": [0.667], "y": [1]},
                                "o": {"x": [0.333], "y": [0]},
                                "t": 0,
                                "s": [angle]
                            },
                            {
                                "t": total_frames,
                                "s": [angle + 360]
                            }
                        ]
                    },
                    "p": {"a": 0, "k": [self.center, self.center, 0]},
                    "a": {"a": 0, "k": [0, 0, 0]},
                    "s": {"a": 0, "k": [100, 100, 100]}
                },
                "ao": 0,
                "shapes": [{
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "sh",
                            "ks": {
                                "a": 0,
                                "k": {
                                    "i": [[0, 0], [0, 0], [0, 0]],
                                    "o": [[0, 0], [0, 0], [0, 0]],
                                    "v": [[0, -40], [-25, 120], [25, 120]],
                                    "c": True
                                }
                            }
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [0, 0, 0, 1]},
                            "o": {"a": 0, "k": 100}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                }],
                "ip": 0,
                "op": total_frames,
                "st": 0,
                "bm": 0
            }
            layers.append(layer)
        
        return layers
    
    def create_sasuke_pattern(self, total_frames):
        """Create Sasuke's Mangekyo pattern (6-pointed star)"""
        layers = []
        
        for i in range(6):
            angle = i * 60
            layer = {
                "ddd": 0,
                "ind": 10 + i,
                "ty": 4,
                "nm": f"Star Point {i+1}",
                "sr": 1,
                "ks": {
                    "o": {"a": 0, "k": 100},
                    "r": {
                        "a": 1,
                        "k": [
                            {
                                "i": {"x": [0.667], "y": [1]},
                                "o": {"x": [0.333], "y": [0]},
                                "t": 0,
                                "s": [angle]
                            },
                            {
                                "t": total_frames,
                                "s": [angle + 360]
                            }
                        ]
                    },
                    "p": {"a": 0, "k": [self.center, self.center, 0]},
                    "a": {"a": 0, "k": [0, 0, 0]},
                    "s": {"a": 0, "k": [100, 100, 100]}
                },
                "ao": 0,
                "shapes": [{
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "rc",
                            "p": {"a": 0, "k": [0, 80]},
                            "s": {"a": 0, "k": [30, 100]},
                            "r": {"a": 0, "k": 0}
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [0, 0, 0, 1]},
                            "o": {"a": 0, "k": 100}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                }],
                "ip": 0,
                "op": total_frames,
                "st": 0,
                "bm": 0
            }
            layers.append(layer)
        
        return layers
    
    def create_kakashi_pattern(self, total_frames):
        """Create Kakashi's Kamui pattern (spiral)"""
        layers = []
        
        # Create spiral triangles
        for i in range(4):
            angle = i * 90
            layer = {
                "ddd": 0,
                "ind": 10 + i,
                "ty": 4,
                "nm": f"Spiral {i+1}",
                "sr": 1,
                "ks": {
                    "o": {"a": 0, "k": 100},
                    "r": {
                        "a": 1,
                        "k": [
                            {
                                "i": {"x": [0.667], "y": [1]},
                                "o": {"x": [0.333], "y": [0]},
                                "t": 0,
                                "s": [angle]
                            },
                            {
                                "t": total_frames,
                                "s": [angle + 360]
                            }
                        ]
                    },
                    "p": {"a": 0, "k": [self.center, self.center, 0]},
                    "a": {"a": 0, "k": [0, 0, 0]},
                    "s": {"a": 0, "k": [100, 100, 100]}
                },
                "ao": 0,
                "shapes": [{
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "sh",
                            "ks": {
                                "a": 0,
                                "k": {
                                    "i": [[0, 0], [0, 0], [0, 0], [0, 0]],
                                    "o": [[0, 0], [0, 0], [0, 0], [0, 0]],
                                    "v": [[0, -30], [40, 50], [0, 150], [-20, 80]],
                                    "c": True
                                }
                            }
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [0, 0, 0, 1]},
                            "o": {"a": 0, "k": 100}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                }],
                "ip": 0,
                "op": total_frames,
                "st": 0,
                "bm": 0
            }
            layers.append(layer)
        
        return layers
    
    def create_obito_pattern(self, total_frames):
        """Create Obito's Kamui pattern (similar to Kakashi but mirrored)"""
        layers = []
        
        for i in range(4):
            angle = i * 90 + 45  # Offset by 45 degrees
            layer = {
                "ddd": 0,
                "ind": 10 + i,
                "ty": 4,
                "nm": f"Kamui Blade {i+1}",
                "sr": 1,
                "ks": {
                    "o": {"a": 0, "k": 100},
                    "r": {
                        "a": 1,
                        "k": [
                            {
                                "i": {"x": [0.667], "y": [1]},
                                "o": {"x": [0.333], "y": [0]},
                                "t": 0,
                                "s": [-angle]  # Opposite direction
                            },
                            {
                                "t": total_frames,
                                "s": [-angle - 360]
                            }
                        ]
                    },
                    "p": {"a": 0, "k": [self.center, self.center, 0]},
                    "a": {"a": 0, "k": [0, 0, 0]},
                    "s": {"a": 0, "k": [100, 100, 100]}
                },
                "ao": 0,
                "shapes": [{
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "sh",
                            "ks": {
                                "a": 0,
                                "k": {
                                    "i": [[0, 0], [0, 0], [0, 0], [0, 0]],
                                    "o": [[0, 0], [0, 0], [0, 0], [0, 0]],
                                    "v": [[0, -30], [20, 80], [0, 150], [-40, 50]],
                                    "c": True
                                }
                            }
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [0, 0, 0, 1]},
                            "o": {"a": 0, "k": 100}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                }],
                "ip": 0,
                "op": total_frames,
                "st": 0,
                "bm": 0
            }
            layers.append(layer)
        
        return layers
    
    def create_pupil_layer(self):
        """Create central black pupil with highlight"""
        return {
            "ddd": 0,
            "ind": 50,
            "ty": 4,
            "nm": "Pupil",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": 100},
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [self.center, self.center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [
                {
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "el",
                            "p": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [80, 80]}
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [0, 0, 0, 1]},
                            "o": {"a": 0, "k": 100}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                },
                {
                    "ty": "gr",
                    "it": [
                        {
                            "ty": "el",
                            "p": {"a": 0, "k": [-10, 10]},
                            "s": {"a": 0, "k": [20, 20]}
                        },
                        {
                            "ty": "fl",
                            "c": {"a": 0, "k": [1, 1, 1, 0.6]},
                            "o": {"a": 0, "k": 60}
                        },
                        {
                            "ty": "tr",
                            "p": {"a": 0, "k": [0, 0]},
                            "a": {"a": 0, "k": [0, 0]},
                            "s": {"a": 0, "k": [100, 100]},
                            "r": {"a": 0, "k": 0},
                            "o": {"a": 0, "k": 100}
                        }
                    ]
                }
            ],
            "ip": 0,
            "op": 300,
            "st": 0,
            "bm": 0
        }
    
    def create_outer_ring_layer(self):
        """Create outer black ring"""
        return {
            "ddd": 0,
            "ind": 60,
            "ty": 4,
            "nm": "Outer Ring",
            "sr": 1,
            "ks": {
                "o": {"a": 0, "k": 100},
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [self.center, self.center, 0]},
                "a": {"a": 0, "k": [0, 0, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "shapes": [{
                "ty": "gr",
                "it": [
                    {
                        "ty": "el",
                        "p": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [440, 440]}
                    },
                    {
                        "ty": "st",
                        "c": {"a": 0, "k": [0, 0, 0, 1]},
                        "o": {"a": 0, "k": 100},
                        "w": {"a": 0, "k": 12}
                    },
                    {
                        "ty": "tr",
                        "p": {"a": 0, "k": [0, 0]},
                        "a": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [100, 100]},
                        "r": {"a": 0, "k": 0},
                        "o": {"a": 0, "k": 100}
                    }
                ]
            }],
            "ip": 0,
            "op": 300,
            "st": 0,
            "bm": 0
        }
    
    def save_lottie(self, lottie_data, filename):
        """Save optimized Lottie JSON"""
        with open(filename, 'w') as f:
            json.dump(lottie_data, f, separators=(',', ':'), indent=None)
        
        import os
        size_kb = os.path.getsize(filename) / 1024
        print(f"✓ Saved {filename} - Size: {size_kb:.2f} KB")
        return size_kb


def main():
    print("=" * 70)
    print("🔥 MANGEKYO SHARINGAN PROFESSIONAL ANIMATION GENERATOR 🔥")
    print("=" * 70)
    print()
    
    sharingan = MangeykoSharingan(size=512)
    
    patterns = {
        "itachi": "Itachi's Mangekyo (3-blade pinwheel)",
        "sasuke": "Sasuke's Mangekyo (6-pointed star)",
        "kakashi": "Kakashi's Kamui (spiral vortex)",
        "obito": "Obito's Kamui (reverse spiral)"
    }
    
    print("Generating Mangekyo Sharingan animations...\n")
    
    total_size = 0
    for pattern_name, description in patterns.items():
        print(f"Creating {description}...")
        lottie_data = sharingan.create_lottie_animation(pattern=pattern_name, duration=3)
        filename = f"mangekyo_{pattern_name}.json"
        size = sharingan.save_lottie(lottie_data, filename)
        total_size += size
        print()
    
    print("=" * 70)
    print("✅ ALL ANIMATIONS GENERATED SUCCESSFULLY!")
    print("=" * 70)
    print(f"\nTotal size: {total_size:.2f} KB")
    print("\nGenerated files:")
    for pattern in patterns.keys():
        print(f"  📄 mangekyo_{pattern}.json")
    
    print("\n🎯 Integration Guide:")
    print("  1. Copy JSON files to: app/src/main/assets/animations/")
    print("  2. Add Lottie dependency to build.gradle:")
    print("     implementation 'com.airbnb.android:lottie:6.1.0'")
    print("  3. Use in XML:")
    print("     <com.airbnb.lottie.LottieAnimationView")
    print("         android:layout_width=\"100dp\"")
    print("         android:layout_height=\"100dp\"")
    print("         app:lottie_fileName=\"animations/mangekyo_itachi.json\"")
    print("         app:lottie_loop=\"true\"")
    print("         app:lottie_autoPlay=\"true\" />")
    print()


if __name__ == "__main__":
    main()
